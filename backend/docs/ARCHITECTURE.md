# System Architecture — Liquidity Risk Optimizer (LRO) Ecosystem

## 1. Executive Overview
The **Liquidity Risk Optimizer (LRO)** ecosystem is a modular, event-driven educational and research simulation platform modeling the interactions between Indian retail/commercial banking institutions, capital market exchanges, and treasury decision-support systems.

```mermaid
graph TD
    subgraph Presentation Layer [JavaFX 23 Desktop Apps]
        APP_A[Bank Simulator UI]
        APP_B[Exchange Simulator UI]
        APP_C[Liquidity Risk Optimizer UI]
    end

    subgraph Messaging Fabric [STOMP Event Broker]
        BROKER[/topic/market.quotes<br>/topic/market.trades<br>/topic/bank.transactions<br>/topic/lro.snapshots<br>/topic/risk.alerts]
    end

    subgraph Backend Engine [Java 25 Multi-Module Reactor]
        BANK_SVC[lro-bank-services<br>Population, Ledger, Loans, Treasury]
        EXCH_SVC[lro-exchange-services<br>Double Auction Engine, Catalogue, Regimes]
        ANALYTICS[lro-analytics<br>MTM Valuation, HQLA, LCR, 99% VaR]
        ORCH[lro-orchestration<br>Ranking Engine, Maker/Checker, Scenarios]
        INGEST[lro-app: Ingestion & Retention<br>OHLCV Validation, 7-Day Pruning]
    end

    subgraph Machine Learning Microservice [Python HTTP Service]
        ML_SRV[inference_server.py :8082<br>Model 1: Equity Momentum<br>Model 2: Slippage & Execution Cost]
    end

    subgraph Persistence Layer [PostgreSQL & Flyway]
        DB_L[bank_large Schema]
        DB_M[bank_medium Schema]
        DB_S[bank_small Schema]
        DB_D[bank_distressed Schema]
        DB_E[exchange Schema]
        DB_O[lro Schema]
    end

    APP_A -->|Queries| BANK_SVC
    APP_B -->|Orders & Quotes| EXCH_SVC
    APP_C -->|Metrics & Actions| ORCH

    BANK_SVC --> BROKER
    EXCH_SVC --> BROKER
    BROKER --> ANALYTICS
    ANALYTICS --> ORCH
    ORCH --> ML_SRV

    BANK_SVC --> DB_L & DB_M & DB_S & DB_D
    EXCH_SVC --> DB_E
    ORCH --> DB_O
```

---

## 2. Multi-Module Breakdown
The backend multi-module Maven structure separates domain contracts, persistence, banking services, matching engine, analytics, and operational orchestration:

| Module | Responsibilities | Key Classes |
|---|---|---|
| `lro-common` | Core monetary arithmetic (`Money` with Banker's rounding), versioned event envelopes (`EventEnvelope`), security/RBAC models (`SecurityContext`), and domain exceptions. | `Money`, `EventEnvelope`, `SecurityContext`, `ValidationException` |
| `lro-persistence` | Multi-schema Flyway database migrations (V1, V2, V3) supporting 4 distinct bank schemas, exchange schema, and LRO analytics schema. | `DatabaseManager`, Flyway migrations V1–V3 |
| `lro-bank-services` | Deterministic synthetic bank population generator (10k, 7.5k, 5k, 5k populations), double-entry transaction ledger balancing, EMI loan amortization, and duration-weighted treasury ladders. | `BankPopulationGenerator`, `DoubleEntryLedgerService`, `LoanNpaService`, `TreasuryLadderService` |
| `lro-exchange-services` | Continuous Price-Time priority double-auction matching engine, 5-depth Level-2 order books, multi-asset instrument catalogue (equities, G-Secs, T-Bills, derivatives), and macro market regimes. | `ContinuousMatchingEngine`, `ExchangeInstrumentCatalogue`, `MarketRegimeEngine` |
| `lro-messaging` | In-memory and STOMP-compatible pub/sub topic broker with outbox queue, idempotency deduplication by `eventId`, and client subscription management. | `StompEventBroker` |
| `lro-analytics` | Real-time mark-to-market lot valuation, Basel III/RBI HQLA classification, illustrative 30-day LCR calculation, and 1-day 99% Parametric Value-at-Risk (VaR). | `PortfolioValuationEngine`, `LiquidityMetricsEngine` |
| `lro-ml-adapter` | Resilient HTTP clients connecting to XGBoost inference microservices with deterministic local fallbacks for high-availability operation. | `Model1MomentumAdapter`, `Model2ExecutionCostAdapter` |
| `lro-orchestration` | Multi-criteria recommendation ranking, Maker/Checker dual-approval authorization workflows, and macro stress scenario shocks. | `RecommendationRankingEngine`, `ScenarioOrchestrator` |
| `lro-app` | Master backend launcher, historical data ingestion controls, 7-day quote retention policies, health telemetry monitoring, and comprehensive acceptance test suite. | `BackendLauncher`, `HistoricalDataIngestionService`, `DataRetentionService`, `SystemHealthMonitor` |

---

## 3. Communication Patterns
1. **Synchronous In-Process API**: Frontend and service modules call strongly-typed Java interfaces and record DTOs.
2. **Asynchronous Topic Pub/Sub**: Real-time quote ticks, trade executions, banking transactions, and liquidity alerts broadcast over STOMP topics.
3. **HTTP REST Boundary**: Optional model inference communication with Python microservice on `http://127.0.0.1:8082` with timeout guards and immediate fallback to deterministic rules.
