# Financial Modelling Assumptions & Plain Language Glossary — Prompt 29

## 1. What is Simulated? Transparent Disclosure

To ensure complete transparency, every data item displayed across the ecosystem is categorized into one of five distinct provenance categories:

| Category | Description | Examples |
|---|---|---|
| **Synthetic Bank Data** | Statistically generated using deterministic random seeds (Seed 42). Plausible Indian demographic and economic profiles. No real customer information. | `CUST-LRG-00001`, `ACC-LRG-00001`, loan schedules, savings balances. |
| **Simulated Market Matching** | High-speed continuous price-time priority matching engine executing orders in simulated time. | Best bids, best asks, order fills, partial fills, resting book depth. |
| **Generated Market Regimes** | Deterministic macro state shifts that alter volatility, spreads, and order arrivals. | `Normal`, `Bull Market`, `Bear Market`, `Market Crash`. |
| **Imported Historical Data** | Publicly accessible historical market end-of-day series imported with provenance validation and licence tracking. | Historical Nifty 50 OHLCV series for Model 1 backtesting. |
| **ML-Generated Estimates** | XGBoost decision-support predictions providing probabilistic estimates of price momentum and slippage. | T+5 expected return percent, expected execution slippage in bps. |

---

## 2. Plain Language Glossary of Financial Concepts

### Banking & Credit Concepts
- **Double-Entry General Ledger**: An accounting invariant where every transaction requires matching debit and credit entries ($\sum \text{Debits} = \sum \text{Credits}$). If cash increases, either liabilities (customer deposits) or equity must increase by the identical amount.
- **IRAC Asset Classification (RBI)**: Prudential norms classifying bank loans based on Days Past Due (DPD):
  - *Standard Asset*: Repayment up to date (0 DPD).
  - *SMA-0 (Special Mention Account 0)*: Principal or interest overdue between 1 and 30 days.
  - *SMA-1*: Overdue between 31 and 60 days.
  - *SMA-2*: Overdue between 61 and 90 days.
  - *Non-Performing Asset (NPA)*: Overdue beyond 90 days, transitioning to *Sub-Standard*, *Doubtful*, and *Loss* requiring provisions.
- **Gross NPA Ratio**: The percentage of a bank's total loan advances that are classified as non-performing. In our simulation, healthy banks maintain 1.8% to 2.8% Gross NPA, while the distressed bank exhibits 14.2% Gross NPA.

### Treasury & Fixed Income Concepts
- **Government Securities (G-Secs)**: Long-term sovereign bonds issued by the Government of India carrying zero credit default risk.
- **Treasury Bills (T-Bills)**: Short-term zero-coupon sovereign money-market debt instruments issued at a discount and redeemed at par (e.g. 91-Day, 182-Day, 364-Day).
- **Clean vs Dirty Price**: The *clean price* is the quoted market price excluding accrued interest. The *dirty price* is the actual cash settlement amount, including interest accrued since the last coupon date.
- **Modified Duration**: A measure of a bond's percentage price sensitivity to a 100 basis point (1%) shift in benchmark interest rates.

### Exchange & Market Microstructure Concepts
- **Bid, Ask, and Spread**: The *bid* is the highest price a buyer is willing to pay; the *ask* (offer) is the lowest price a seller will accept. The difference is the *bid-ask spread*, representing the immediate cost of liquidity.
- **Level-2 Order Book**: A real-time aggregated schedule of resting buy and sell limit orders ranked by price and timestamp across the top 5 price levels.
- **Price-Time Priority**: The matching rule whereby orders with the best price match first; among orders at the same price, the earliest submitted order matches first.
- **Slippage**: The difference between the expected transaction price (such as the prevailing mid-quote) and the actual executed average fill price resulting from order size exceeding resting market depth.
- **Square-Root Law of Market Impact**: Empirical financial law where execution slippage scales proportionally with the square root of order size relative to Average Daily Volume ($\text{Impact} \propto \sigma \sqrt{\frac{Q}{ADV}}$).
