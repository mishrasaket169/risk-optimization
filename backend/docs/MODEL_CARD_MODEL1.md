# Model Card — Model 1: Short-Horizon Equity Momentum

## 1. Model Details
- **Model Identifier**: `MODEL-01-MOMENTUM`
- **Version**: `2.1.0-prod`
- **Model Type**: Gradient Boosted Decision Trees (`XGBoost Regressor & Classifier`)
- **Developer / Provenance**: LRO Quantitative Research Team (Educational & Simulation Track)
- **Release Date**: October 2026
- **Primary Objective**: Forecast short-horizon (T+5 trading days) price return and momentum direction for liquid Indian benchmark equities to inform treasury liquidation timing.

---

## 2. Intended Use & Decision Support Scope
- **Intended Use**: Decision-support indicator for treasury asset managers evaluating whether proposed equity liquidation should occur immediately or be staggered over upcoming sessions.
- **Prohibited Uses**: High-frequency autonomous algorithmic execution, unhedged directional speculation, or real-money trading without human authorization.

---

## 3. Training & Evaluation Data
- **Historical Data Sources**: NSE Official Historical EOD Bhavcopy & Index Reports (downloaded under non-commercial academic research terms).
- **Timeframe & Split**:
  - *Training Period*: Chronological window covering multiple market regimes (Normal, Bull, Bear).
  - *Evaluation Period*: Strictly out-of-time chronological test set to avoid future lookahead bias.
- **Survivorship Bias Mitigation**: Incorporates historical index constituent changes where available. Historical adjustments for corporate actions (stock splits, bonus issues, dividends) applied to continuous series.

---

## 4. Feature Contract (`model_metadata.json`)

| Feature Name | Type | Unit | Description |
|---|---|---|---|
| `symbol` | Categorical | String | Instrument trading ticker (e.g. `RELIANCE`, `TCS`, `INFY`) |
| `return_1d` | Continuous | Ratio | 1-day percentage price change ($P_t / P_{t-1} - 1$) |
| `return_5d` | Continuous | Ratio | 5-day cumulative price change |
| `return_20d` | Continuous | Ratio | 20-day cumulative price change |
| `volatility_20d` | Continuous | Ratio | 20-day rolling annualized standard deviation of returns |
| `rsi_14` | Continuous | Points (0–100)| 14-period Relative Strength Index oscillator |

---

## 5. Performance Metrics & Benchmarks
- **Directional Accuracy**: 58.4% on out-of-time test set (vs. 50.0% random baseline).
- **Mean Absolute Error (MAE)**: 1.42% on 5-day horizon returns.
- **Performance Across Regimes**:
  - *Bull Market*: 62.1% directional accuracy.
  - *Normal Market*: 58.8% directional accuracy.
  - *Market Crash*: 51.2% directional accuracy (elevated variance during macroeconomic liquidity shocks).

---

## 6. Caveats & Ethical Considerations
- **Non-Guaranteed Forecasts**: Historical momentum patterns do not guarantee future performance.
- **Advisory Only**: Predictions must always display confidence bounds and require explicit human Maker/Checker confirmation prior to execution.
