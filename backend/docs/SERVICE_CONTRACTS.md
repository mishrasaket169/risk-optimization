# Service Contracts & Cross-Application Consistency — Prompt 29

## 1. Domain Ownership Boundaries
To prevent architectural coupling and conflicting state updates, subsystem ownership is strictly segregated:
- **Bank Simulator Subsystem**: Exclusively owns customer deposits, loan contracts, NPA state, bank treasury positions, and double-entry general ledgers. External systems may never write to `bank_*` tables directly.
- **Exchange Simulator Subsystem**: Exclusively owns order books, quotes, trade executions, instrument catalogues, and market regimes.
- **Liquidity Risk Optimizer Subsystem**: Ingests market and banking events asynchronously, calculates mark-to-market positions, computes risk metrics, generates decision-support recommendations, and manages the Maker/Checker audit trail.

---

## 2. Event Envelope Schema (`EventEnvelope<T>`)
All inter-application asynchronous messaging uses versioned JSON event envelopes:

```json
{
  "eventId": "3c91f1b2-3e28-4e89-8d75-7b56a45b6f01",
  "eventType": "TRADE_EXECUTED",
  "schemaVersion": "1.0.0",
  "occurredAt": "2026-10-11T13:30:00.124Z",
  "producer": "ExchangeSimulator",
  "correlationId": "ORD-RELIANCE-98124",
  "entityId": "TRADE-891241",
  "payload": {
    "symbol": "RELIANCE",
    "buyOrderId": "BID-01",
    "sellOrderId": "ASK-01",
    "quantity": 200,
    "executionPrice": {
      "amount": "2850.00",
      "currency": "INR"
    }
  }
}
```

### Destination Topic Catalogue

| Topic Destination | Producer | Payload DTO / Event | Purpose |
|---|---|---|---|
| `/topic/market.quotes` | Exchange Simulator | Best Bid/Ask & LTP updates | Streams live price ticks for portfolio MTM revaluation |
| `/topic/market.trades` | Exchange Simulator | `TradeExecution` | Notifies fills, triggers lot adjustments and cash settlement |
| `/topic/market.regime` | Exchange Simulator / Orchestrator | `MarketRegimeChangeEvent` | Broadcasts regime shifts (`Normal`, `Bull`, `Bear`, `Crash`) |
| `/topic/bank.transactions`| Bank Simulator | `JournalEntry` list | Reports large customer withdrawals, deposit inflows, or loan drawdowns |
| `/topic/lro.snapshots` | LRO Analytics Engine | `LiquiditySnapshot` | Broadcasts updated HQLA, cash, and illustrative LCR/NSFR |
| `/topic/risk.alerts` | LRO Analytics / Orchestrator | `RiskAlert` | Warns operators of LCR breaches, VaR spikes, or macro shocks |

---

## 3. Python ML Microservice REST Contracts
When local ML inference is enabled on `http://127.0.0.1:8082`, the Java backend invokes the following REST endpoints:

### Model 1: Equity Momentum (`POST /predict/momentum`)
- **Request Body**:
  ```json
  {
    "symbol": "RELIANCE",
    "return_1d": 0.012,
    "return_5d": 0.035,
    "return_20d": 0.052,
    "volatility_20d": 0.018,
    "rsi_14": 58.4
  }
  ```
- **Response Body**:
  ```json
  {
    "modelId": "MODEL-01-MOMENTUM",
    "modelVersion": "2.1.0-prod",
    "forecastHorizon": "T+5 Trading Days",
    "predictedReturnPercent": 2.15,
    "confidenceScore": 0.88,
    "limitations": "Model trained on historical OHLCV data. Past momentum does not guarantee future price returns."
  }
  ```

### Model 2: Execution Cost & Slippage (`POST /predict/execution-cost`)
- **Request Body**:
  ```json
  {
    "symbol": "GS-7.18-2033",
    "category": "G-Sec",
    "proposed_quantity": 100000,
    "current_ltp": 100.84,
    "bid_ask_spread_bps": 2.0,
    "top5_depth_quantity": 500000,
    "order_to_adv_ratio": 0.02,
    "market_regime": "Normal"
  }
  ```
- **Response Body**:
  ```json
  {
    "modelId": "MODEL-02-EXEC-COST",
    "modelVersion": "1.4.2-prod",
    "expectedSlippageBps": 3.85,
    "estimatedExecutionCostINR": 38823.40,
    "netAchievableProceedsINR": 10045176.60,
    "completionProbability": 0.96,
    "limitations": "Estimates derived from synthetic order-matching simulator dynamics. Actual broker execution may vary."
  }
  ```

---

## 4. Idempotency & Fault Tolerance
1. **Deduplication Filter**: Every incoming event has a unique UUID `eventId`. The STOMP broker caches processed event IDs in an atomic set and silently drops duplicates.
2. **Deterministic Fallbacks**: If the ML microservice is unreachable, adapters immediately switch to deterministic financial formulas without disrupting system responsiveness.
