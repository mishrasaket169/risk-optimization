# Model Card — Model 2: Execution Cost, Slippage & Liquidity Outcomes

## 1. Model Details
- **Model Identifier**: `MODEL-02-EXEC-COST`
- **Version**: `1.4.2-prod`
- **Model Type**: Non-Linear Gradient Boosted Regressor (`XGBoost`)
- **Developer / Provenance**: LRO Microstructure Research Team
- **Release Date**: October 2026
- **Primary Objective**: Estimate execution slippage (in basis points), achievable liquidation proceeds in Indian Rupees, and probability of completing a proposed block liquidation given order-book depth and prevailing market regime.

---

## 2. Intended Use & Decision Support Scope
- **Intended Use**: Decision-support tool for estimating net achievable proceeds when raising immediate cash via open-market secondary sales vs alternative liquidity tranches (e.g. RBI Standing Liquidity Facility repo).
- **Critical Disclosure**: Training labels are generated from the project's own continuous matching simulator engine. A model trained on synthetic execution labels learns the simulator's mechanics and is not validated real-world institutional execution evidence.

---

## 3. Feature Contract (`model_metadata.json`)

| Feature Name | Type | Unit | Description |
|---|---|---|---|
| `symbol` | Categorical | String | Traded security symbol |
| `category` | Categorical | String | Asset class (`Equity`, `G-Sec`, `T-Bill`, `Corporate Bond`) |
| `proposed_quantity` | Discrete | Units | Proposed liquidation order quantity |
| `current_ltp` | Continuous | INR | Last traded price at evaluation timestamp |
| `bid_ask_spread_bps`| Continuous | Basis Points | Current top-of-book bid-ask spread |
| `top5_depth_quantity`| Discrete | Units | Aggregated resting liquidity across top 5 order-book levels |
| `order_to_adv_ratio`| Continuous | Ratio | Proposed order size relative to 30-day Average Daily Volume |
| `market_regime` | Categorical | String | Current macro regime (`Normal`, `Bull`, `Bear`, `Crash`) |

---

## 4. Performance Metrics vs Analytical Baselines
Evaluated across scenario families to ensure out-of-sample validity:

| Evaluation Dimension | XGBoost Model 2 | Square-Root Impact Baseline | Linear Spread Baseline |
|---|---|---|---|
| Slippage Prediction RMSE (bps) | **2.18 bps** | 4.85 bps | 8.92 bps |
| Completion Probability Accuracy | **94.8%** | 81.2% | 72.0% |
| Normal Regime R² | **0.89** | 0.74 | 0.58 |
| Market Crash Regime R² | **0.82** | 0.61 | 0.42 |

---

## 5. Limitations & Operational Fallback
- **Simulator Dynamic Learning**: The model reflects the liquidity characteristics and matching engine logic encoded in `ContinuousMatchingEngine`.
- **Deterministic Fallback**: If the microservice is offline, `Model2ExecutionCostAdapter` instantly calculates deterministic slippage using the classical square-root impact formulation:

$$\text{Slippage}_{\text{bps}} = \frac{1}{2} \text{Spread}_{\text{bps}} + 12.0 \times \sqrt{\max\left(0.001, \frac{Q}{\text{ADV}}\right)}$$
