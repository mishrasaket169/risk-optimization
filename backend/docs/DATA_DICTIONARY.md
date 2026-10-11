# Data Dictionary — Liquidity Risk Optimizer (LRO)

## 1. Schema Topology Inventory
The persistence layer utilizes a normalized multi-schema layout managed by Flyway migrations (`V1__init_bank_schemas.sql`, `V2__init_exchange_schemas.sql`, `V3__init_lro_schemas.sql`).

| Schema Name | Target Audience / Purpose | Target Seed Population |
|---|---|---|
| `bank_large` | Large Healthy Private/Public Bank | 10,000 customers, 1,000 loans, 500 G-Sec lots |
| `bank_medium` | Medium Sized Regional Bank | 7,500 customers, 750 loans, 350 G-Sec lots |
| `bank_small` | Small Specialized Healthy Bank | 5,000 customers, 500 loans, 200 G-Sec lots |
| `bank_distressed` | Stressed Bank (Elevated NPAs & Deposit Run Risk) | 5,000 customers, 1,200 loans (14.2% Gross NPA), 100 G-Sec lots |
| `exchange` | Continuous Double-Auction Multi-Asset Exchange | 11+ Asset families (Equities, G-Secs, T-Bills, Bonds, Futures, Options) |
| `lro` | Liquidity Risk Optimizer & Treasury Audit | Real-time snapshots, recommendations, alerts, and append-only audit log |

---

## 2. Bank Schema Entity Specifications (`bank_*`)

### `customers`
- `customer_id` (`VARCHAR(64)` PRIMARY KEY): Synthetic customer identifier (e.g. `CUST-LRG-1001`).
- `full_name` (`VARCHAR(128)` NOT NULL): Synthetic customer legal name.
- `segment` (`VARCHAR(32)` NOT NULL): Randomly distributed across `Retail`, `SME`, `Corporate`, `Wealth`.
- `kyc_status` (`VARCHAR(32)` NOT NULL): `Verified`, `Not Verified`.
- `location_category` (`VARCHAR(32)` NOT NULL): `Tier 1`, `Tier 2`, `Tier 3`, `Rural`.
- `risk_category` (`VARCHAR(16)` NOT NULL): Internal credit score tier (`Low`, `Medium`, `High`).
- `branch_name` (`VARCHAR(64)` NOT NULL): Originating bank branch name.
- **Population Scale**:
  - `bank_large`: 10,000 customers
  - `bank_medium`: 7,500 customers
  - `bank_small`: 5,000 customers
  - `bank_distressed`: 4,000 customers


### `accounts`
- `account_id` (`VARCHAR(32)` PRIMARY KEY): Synthetic account number (e.g. `ACC-LRG-00001`).
- `customer_id` (`VARCHAR(32)` REFERENCES `customers`): Parent customer reference.
- `account_type` (`VARCHAR(32)` NOT NULL): `SAVINGS`, `CURRENT`, `SALARY`, `ESCROW`.
- `balance_inr` (`NUMERIC(18, 2)` NOT NULL): Current available balance in Indian Rupees.
- `interest_rate_pct` (`NUMERIC(5, 2)` NOT NULL): Annual deposit interest rate.
- `status` (`VARCHAR(16)` NOT NULL): `ACTIVE`, `DORMANT`, `FROZEN`.

### `loans`
- `loan_id` (`VARCHAR(32)` PRIMARY KEY): Loan contract identifier.
- `customer_id` (`VARCHAR(32)` REFERENCES `customers`): Borrower customer reference.
- `loan_type` (`VARCHAR(32)` NOT NULL): `HOME_LOAN`, `AUTO_LOAN`, `PERSONAL_LOAN`, `COMMERCIAL_WC`.
- `principal_inr` (`NUMERIC(18, 2)` NOT NULL): Sanctioned loan principal amount.
- `outstanding_inr` (`NUMERIC(18, 2)` NOT NULL): Current amortized balance.
- `interest_rate_pct` (`NUMERIC(5, 2)` NOT NULL): Annual contractual lending yield.
- `tenure_months` (`INTEGER` NOT NULL): Total repayment tenure.
- `dpd` (`INTEGER` NOT NULL DEFAULT 0): Days Past Due tracking.
- `irac_status` (`VARCHAR(16)` NOT NULL): RBI IRAC classification (`STANDARD`, `SMA_0`, `SMA_1`, `SMA_2`, `SUB_STANDARD`, `DOUBTFUL`, `LOSS`).

### `securities_holdings`
- `lot_id` (`VARCHAR(32)` PRIMARY KEY): Unique inventory lot identifier.
- `symbol` (`VARCHAR(32)` NOT NULL): Exchange instrument ticker.
- `isin` (`VARCHAR(32)` NOT NULL): Simulated ISIN.
- `category` (`VARCHAR(32)` NOT NULL): `G-Sec`, `T-Bill`, `Corporate Bond`, `Equity`.
- `quantity` (`BIGINT` NOT NULL): Lot quantity in units/shares.
- `book_value_inr` (`NUMERIC(18, 2)` NOT NULL): Amortized cost basis per unit.
- `current_market_price_inr` (`NUMERIC(18, 2)` NOT NULL): Latest mark-to-market valuation.
- `is_encumbered` (`BOOLEAN` NOT NULL DEFAULT FALSE): Repo pledged or statutory reserve flag.

### `transaction_ledger`
- `entry_id` (`VARCHAR(64)` PRIMARY KEY): Double-entry ledger entry identifier.
- `transaction_id` (`VARCHAR(64)` NOT NULL): Transaction batch correlation ID.
- `account_type` (`VARCHAR(64)` NOT NULL): Chart of accounts target (`CASH_RESERVE`, `DEPOSITS`, etc.).
- `debit_inr` (`NUMERIC(18, 2)` NOT NULL): Debit leg amount.
- `credit_inr` (`NUMERIC(18, 2)` NOT NULL): Credit leg amount.
- `status` (`VARCHAR(16)` NOT NULL): `POSTED`, `SETTLED`.

---

## 3. Exchange Schema Entity Specifications (`exchange`)

### `instruments`
- `symbol` (`VARCHAR(32)` PRIMARY KEY): Exchange trading ticker (e.g. `RELIANCE`, `GS-7.18-2033`).
- `isin` (`VARCHAR(32)` NOT NULL): Simulated ISIN with `-SIM` suffix.
- `category` (`VARCHAR(32)` NOT NULL): Instrument family.
- `tick_size` (`NUMERIC(8, 4)` NOT NULL): Minimum allowable price movement.
- `lot_size` (`INTEGER` NOT NULL): Standard contract lot size.
- `last_traded_price` (`NUMERIC(18, 2)` NOT NULL): Most recent execution price.

### `trades`
- `trade_id` (`VARCHAR(64)` PRIMARY KEY): Trade fill identifier.
- `symbol` (`VARCHAR(32)` REFERENCES `instruments`): Traded security.
- `buy_order_id` (`VARCHAR(64)` NOT NULL): Aggressor or resting bid order ID.
- `sell_order_id` (`VARCHAR(64)` NOT NULL): Aggressor or resting ask order ID.
- `quantity` (`BIGINT` NOT NULL): Executed quantity.
- `execution_price` (`NUMERIC(18, 2)` NOT NULL): Fill price.
- `executed_at` (`TIMESTAMP WITH TIME ZONE` NOT NULL): Execution epoch timestamp.

---

## 4. LRO Schema Entity Specifications (`lro`)

### `liquidity_snapshots`
- `snapshot_id` (`VARCHAR(64)` PRIMARY KEY): Periodic liquidity snapshot identifier.
- `bank_id` (`VARCHAR(32)` NOT NULL): Target institution ID.
- `cash_available_inr` (`NUMERIC(18, 2)` NOT NULL): Verified ledger cash balance.
- `liquid_assets_hqla_inr` (`NUMERIC(18, 2)` NOT NULL): Post-haircut eligible HQLA.
- `educational_lcr_pct` (`NUMERIC(8, 2)` NOT NULL): Illustrative Liquidity Coverage Ratio.
- `educational_nsfr_pct` (`NUMERIC(8, 2)` NOT NULL): Illustrative Net Stable Funding Ratio.

### `recommendations`
- `recommendation_id` (`VARCHAR(64)` PRIMARY KEY): Treasury recommendation identifier.
- `bank_id` (`VARCHAR(32)` NOT NULL): Target institution ID.
- `proposed_action` (`VARCHAR(64)` NOT NULL): Action type (`Bilateral Interbank Repo`, `Open Market Sale`, etc.).
- `expected_gross_proceeds_inr` (`NUMERIC(18, 2)` NOT NULL): Estimated proceeds before market impact.
- `estimated_cost_inr` (`NUMERIC(18, 2)` NOT NULL): Model 2 execution cost & slippage estimate.
- `status` (`VARCHAR(32)` NOT NULL): Dual-approval status (`Pending Review`, `Approved`, `Executed`, `Rejected`).

### `audit_log`
- `audit_id` (`VARCHAR(64)` PRIMARY KEY): Append-only audit record identifier.
- `actor_user_id` (`VARCHAR(64)` NOT NULL): User identity performing the action.
- `role_at_execution` (`VARCHAR(64)` NOT NULL): RBAC role at action time.
- `action_name` (`VARCHAR(64)` NOT NULL): Operation name (`APPROVE_RECOMMENDATION`, `INCUR_DEPOSIT_RUN`, etc.).
- `details_json` (`TEXT` NOT NULL): Complete serialised parameter snapshot.
- `signature_hash` (`VARCHAR(64)` NOT NULL): SHA-256 tamper-evident integrity hash.
