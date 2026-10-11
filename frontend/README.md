# Liquidity Risk Optimizer (LRO) — JavaFX Desktop Frontend Shell

## Architectural Overview
This module houses the Java 25 presentation layer and desktop shell for the three-application educational/research simulation ecosystem:
1. **Bank Simulator**: Models four fictional Indian banks, their customers, accounts, deposits, loans, IRAC NPAs, treasury holdings, general ledger, and cash-flow maturity ladders.
2. **Exchange Simulator**: Continuous double auction matching shell with multi-asset catalogue (Equities, G-Secs, T-Bills, Corporate Bonds, Futures, Options, FX, Commodities), 5-depth Level-2 order book, and market regime dynamics.
3. **Liquidity Risk Optimizer (LRO)**: Treasury decision-support workspace providing portfolio mark-to-market revaluations, educational LCR/NSFR indicators, Model 1 & 2 ML transparency cards, and human authorization queues.

---

## Directory & Package Map
```
frontend/
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/com/lro/frontend/
    │   │   ├── AppLauncher.java                     (Bootstrap launcher avoiding module path issues)
    │   │   ├── MainApp.java                         (Shared shell, navigation rail, and app switcher)
    │   │   ├── common/
    │   │   │   ├── Formatters.java                  (High-precision INR, Crores, Lakhs, bps & dates)
    │   │   │   └── UIComponents.java                (Design system KPI cards, chips, modal glossaries)
    │   │   ├── bank/
    │   │   │   ├── model/BankModels.java            (BankProfile, Customer, Account, Loan, Treasury)
    │   │   │   └── service/
    │   │   │       ├── BankService.java             (Clean service boundary interface)
    │   │   │       └── MockBankService.java         (Deterministic fixtures for all 4 fictional banks)
    │   │   ├── exchange/
    │   │   │   ├── model/ExchangeModels.java        (MarketOverview, Instrument, OrderBookLevel, Order)
    │   │   │   └── service/
    │   │   │       ├── ExchangeService.java         (Exchange presentation boundary interface)
    │   │   │       └── MockExchangeService.java     (Simulated multi-asset order book & blotters)
    │   │   ├── lro/
    │   │   │   ├── model/LroModels.java             (LiquiditySnapshot, AssetMetric, ModelInsight, Recs)
    │   │   │   └── service/
    │   │   │       ├── LroService.java              (Decision-support service boundary interface)
    │   │   │       └── MockLroService.java          (Deterministic risk metrics, ML insights, approvals)
    │   │   └── views/
    │   │       ├── BankViews.java                   (Bank overview, customers, loans, treasury, ledger)
    │   │       ├── ExchangeViews.java               (Market overview, catalogue, depth, blotter, regimes)
    │   │       └── LroViews.java                    (LRO dashboard, MTM portfolio, insights, stress tests)
    │   └── resources/com/lro/frontend/
    │       └── css/style.css                        (High-contrast slate/indigo financial design system)
    └── test/java/com/lro/frontend/
        └── FrontendUnitTests.java                   (Verification of all 8 prompts' requirements)
```

---

## Running the Application
### Prerequisites
- JDK 25 (OpenJDK 25 LTS installed)
- Apache Maven 3.9+

### Build and Test
```bash
mvn clean test
```

### Run the Interactive Desktop Application
```bash
mvn javafx:run
```
Or execute directly using the launcher:
```bash
mvn compile exec:java -Dexec.mainClass="com.lro.frontend.AppLauncher"
```

---

## Seam for Backend Integration (Prompts 11-25)
The presentation layer is decoupled from backend storage and network brokers through clean interfaces:
- `BankService` → Will be implemented by `PostgreSqlBankService` / `StompBankClient`
- `ExchangeService` → Will connect to continuous matching engine over STOMP
- `LroService` → Will connect to LRO calculation engine and ML model adapter.
All monetary amounts are strictly represented with `BigDecimal` and all timestamps with `Instant` / `OffsetDateTime`.
