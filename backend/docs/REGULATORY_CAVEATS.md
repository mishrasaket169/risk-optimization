# Regulatory Caveats & Financial Calculation Review — Prompt 29

> [!WARNING]
> **SIMULATION & RESEARCH NOTICE**:
> The Liquidity Risk Optimizer (LRO) ecosystem is strictly an **educational and academic research simulator**. It is NOT a licensed banking software, real payment gateway, or certified regulatory reporting system. All bank entities, accounts, loan balances, transactions, and exchange orders are **fictional and synthetic**.

---

## 1. Liquidity Coverage Ratio (LCR) Semantics
Under RBI Basel III guidelines (RBI Master Direction on Basel III Liquidity Coverage Ratio), LCR measures a bank's capacity to survive a 30-day severe liquidity stress scenario:

$$\text{LCR} = \frac{\text{Total Eligible High-Quality Liquid Assets (HQLA)}}{\text{Total Net Stressed Cash Outflows over 30 Days}} \times 100\%$$

### Ecosystem Asset Categorization & Haircuts
1. **Level 1 HQLA (0.0% – 0.5% Haircut)**:
   - Cash Reserves & Verified Central Bank Balances (0% Haircut).
   - Sovereign Government Securities (G-Secs) and Treasury Bills (T-Bills) eligible under SLR/MSF (0.5% educational buffer haircut).
2. **Level 2A HQLA (15.0% Haircut)**:
   - AAA-rated Corporate Bonds and Public Financial Institution Debt with active secondary market trading.
3. **Level 2B HQLA (50.0% Haircut)**:
   - Liquid common equity shares included in benchmark indices (e.g., Nifty 50 constituents) subject to a 15% maximum cap of total HQLA.
4. **Non-HQLA Assets (100.0% Deduction)**:
   - Unlisted debt, unrated commercial paper, encumbered repo securities, non-performing loans, and physical real estate collateral.

---

## 2. Cash Outflow & Run-Off Assumptions
The educational model adopts the following standardized run-off factors over a 30-day stressed horizon:

| Liability Type | Baseline Run-Off | Macro Stressed Run-Off | Rationale |
|---|---|---|---|
| Stable Retail Deposits | 5.0% | 12.0% | Insured deposits covered by DICGC (₹ 5 Lakh limit). |
| Less Stable Retail Deposits | 10.0% | 20.0% | High-net-worth internet banking accounts without sticky relationships. |
| Operational Corporate Deposits | 25.0% | 40.0% | Working capital clearing balances held by business entities. |
| Non-Operational Wholesale Funding | 40.0% | 75.0% | Short-term interbank borrowings and certificates of deposit. |

---

## 3. Parametric Value-at-Risk (VaR) vs Liquidity Risk
The system computes **1-Day 99% Parametric Value-at-Risk (VaR)** on market-sensitive liquid holdings:

$$\text{VaR}_{99} = 2.3263 \times \text{Portfolio Market Value} \times \frac{\sigma_{\text{annual}}}{\sqrt{252}}$$

### Important Caveats
- **Market Risk vs Liquidity Risk**: VaR estimates potential mark-to-market loss under normal market liquidity conditions. It does **not** reflect liquidation cost, order-book depth depletion, or fire-sale discounts during market freezes.
- **Normal Distribution Assumption**: Parametric VaR assumes price returns follow a Gaussian distribution. Tail risks and black-swan shocks are addressed via the **Scenario Orchestrator**, which applies deterministic macro crashes (-30% equity shocks, +150 bps yield spikes).

---

## 4. Human-in-the-Loop & Decision Support Safeguards
- **No Autonomous Execution**: The system is structurally prohibited from executing trades or movements autonomously.
- **Dual Authorization (Maker/Checker)**: Every proposed recommendation requires initiation by a Treasury Analyst (`MAKER`) and explicit authorization by a Treasury Supervisor (`CHECKER`).
- **Explainability**: Every recommendation presents its forecast horizon, model version, confidence bounds, estimated slippage, and explicit financial rationale.
