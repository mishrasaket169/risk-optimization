-- V4: Add primary keys to replicated bank schemas and populate initial baseline seed data

-- 1. Ensure primary keys on medium, small, and distressed bank schemas
DO $$
BEGIN
    -- bank_medium
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'customers_pkey' AND connamespace = 'bank_medium'::regnamespace) THEN
        ALTER TABLE bank_medium.customers ADD PRIMARY KEY (customer_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'accounts_pkey' AND connamespace = 'bank_medium'::regnamespace) THEN
        ALTER TABLE bank_medium.accounts ADD PRIMARY KEY (account_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'deposits_pkey' AND connamespace = 'bank_medium'::regnamespace) THEN
        ALTER TABLE bank_medium.deposits ADD PRIMARY KEY (deposit_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'loans_pkey' AND connamespace = 'bank_medium'::regnamespace) THEN
        ALTER TABLE bank_medium.loans ADD PRIMARY KEY (loan_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'treasury_holdings_pkey' AND connamespace = 'bank_medium'::regnamespace) THEN
        ALTER TABLE bank_medium.treasury_holdings ADD PRIMARY KEY (holding_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ledger_entries_pkey' AND connamespace = 'bank_medium'::regnamespace) THEN
        ALTER TABLE bank_medium.ledger_entries ADD PRIMARY KEY (entry_id);
    END IF;

    -- bank_small
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'customers_pkey' AND connamespace = 'bank_small'::regnamespace) THEN
        ALTER TABLE bank_small.customers ADD PRIMARY KEY (customer_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'accounts_pkey' AND connamespace = 'bank_small'::regnamespace) THEN
        ALTER TABLE bank_small.accounts ADD PRIMARY KEY (account_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'deposits_pkey' AND connamespace = 'bank_small'::regnamespace) THEN
        ALTER TABLE bank_small.deposits ADD PRIMARY KEY (deposit_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'loans_pkey' AND connamespace = 'bank_small'::regnamespace) THEN
        ALTER TABLE bank_small.loans ADD PRIMARY KEY (loan_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'treasury_holdings_pkey' AND connamespace = 'bank_small'::regnamespace) THEN
        ALTER TABLE bank_small.treasury_holdings ADD PRIMARY KEY (holding_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ledger_entries_pkey' AND connamespace = 'bank_small'::regnamespace) THEN
        ALTER TABLE bank_small.ledger_entries ADD PRIMARY KEY (entry_id);
    END IF;

    -- bank_distressed
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'customers_pkey' AND connamespace = 'bank_distressed'::regnamespace) THEN
        ALTER TABLE bank_distressed.customers ADD PRIMARY KEY (customer_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'accounts_pkey' AND connamespace = 'bank_distressed'::regnamespace) THEN
        ALTER TABLE bank_distressed.accounts ADD PRIMARY KEY (account_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'deposits_pkey' AND connamespace = 'bank_distressed'::regnamespace) THEN
        ALTER TABLE bank_distressed.deposits ADD PRIMARY KEY (deposit_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'loans_pkey' AND connamespace = 'bank_distressed'::regnamespace) THEN
        ALTER TABLE bank_distressed.loans ADD PRIMARY KEY (loan_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'treasury_holdings_pkey' AND connamespace = 'bank_distressed'::regnamespace) THEN
        ALTER TABLE bank_distressed.treasury_holdings ADD PRIMARY KEY (holding_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ledger_entries_pkey' AND connamespace = 'bank_distressed'::regnamespace) THEN
        ALTER TABLE bank_distressed.ledger_entries ADD PRIMARY KEY (entry_id);
    END IF;
END $$;

-- 2. Seed Exchange Instruments
INSERT INTO exchange.instruments (symbol, isin, name, category, sector, tick_size, lot_size, last_traded_price, day_change_pct, bid_price, ask_price, day_volume, day_turnover_inr, coupon_or_yield_pct, maturity_or_expiry, strike_price, option_type)
VALUES
('RELIANCE', 'INE002A01018-SIM', 'Reliance Industries (Simulated)', 'Equity', 'Energy', 0.05, 1, 2840.50, 0.45, 2840.25, 2840.75, 2100000, 5965000000.00, NULL, NULL, NULL, NULL),
('HDFCBANK', 'INE040A01034-SIM', 'HDFC Bank Ltd (Simulated)', 'Equity', 'Banking', 0.05, 1, 1685.20, -0.28, 1685.00, 1685.50, 3400000, 5729000000.00, NULL, NULL, NULL, NULL),
('INFY', 'INE009A01021-SIM', 'Infosys Ltd (Simulated)', 'Equity', 'IT', 0.05, 1, 1820.75, 0.85, 1820.50, 1821.00, 1850000, 3368000000.00, NULL, NULL, NULL, NULL),
('TCS', 'INE467B01029-SIM', 'Tata Consultancy Services (Simulated)', 'Equity', 'IT', 0.05, 1, 4150.00, 0.12, 4149.50, 4150.50, 920000, 3818000000.00, NULL, NULL, NULL, NULL),
('GS-7.18-2033', 'IN0020230085-SIM', '7.18% GS 2033 (10-Yr Benchmark)', 'G-Sec', 'Sovereign', 0.01, 10000, 100.84, 0.05, 100.82, 100.86, 12500000, 12605000000.00, 7.18, '2033-08-14', NULL, NULL),
('GS-7.26-2032', 'IN0020220011-SIM', '7.26% GS 2032', 'G-Sec', 'Sovereign', 0.01, 10000, 100.92, -0.02, 100.90, 100.94, 8400000, 8477000000.00, 7.26, '2032-02-06', NULL, NULL),
('TBILL-91D', 'IN002024X025-SIM', '91-Day Treasury Bill', 'T-Bill', 'Sovereign Money Market', 0.01, 25000, 98.42, 0.01, 98.40, 98.44, 45000000, 44289000000.00, 6.72, '2026-12-15', NULL, NULL),
('TBILL-364D', 'IN002024Y010-SIM', '364-Day Treasury Bill', 'T-Bill', 'Sovereign Money Market', 0.01, 25000, 93.65, 0.02, 93.63, 93.67, 32000000, 29968000000.00, 6.85, '2027-09-20', NULL, NULL),
('HDFC-BOND-8.25', 'INE040A08010-SIM', 'HDFC Ltd 8.25% 2028 NCD', 'Corporate Bond', 'Financials', 0.01, 1000, 101.40, 0.00, 101.20, 101.60, 450000, 456300000.00, 8.25, '2028-06-30', NULL, NULL),
('NIFTY-FUT-DEC', 'INF767A01010-SIM', 'NIFTY 50 Futures Dec-2026', 'Futures', 'Index Derivative', 0.05, 50, 24680.00, 0.35, 24675.00, 24685.00, 5200000, 64168000000.00, NULL, '2026-12-31', NULL, NULL),
('BANKNIFTY-FUT-DEC', 'INF767A01020-SIM', 'BANK NIFTY Futures Dec-2026', 'Futures', 'Banking Derivative', 0.05, 15, 52400.00, 0.22, 52390.00, 52410.00, 4100000, 32226000000.00, NULL, '2026-12-31', NULL, NULL),
('NIFTY-24500-CE', 'INF767A01030-SIM', 'NIFTY 24500 Call Option', 'Options', 'Index Options', 0.05, 50, 295.40, 4.20, 295.00, 295.80, 8900000, 1314500000.00, NULL, '2026-12-31', 24500.00, 'CE'),
('USDINR-FUT-DEC', 'INF767A01040-SIM', 'USD/INR Currency Future Dec-2026', 'FX', 'Currency Derivative', 0.0025, 1000, 84.1250, 0.04, 84.1225, 84.1275, 12000000, 10095000000.00, NULL, '2026-12-29', NULL, NULL)
ON CONFLICT (symbol) DO NOTHING;

-- 3. Seed Bank Large (Apex National Bank)
INSERT INTO bank_large.customers (customer_id, full_name, segment, kyc_status, risk_category, branch_name)
VALUES
('CUST-LRG-1001', 'Rajesh Sharma', 'Retail', 'KYC Tier 1', 'Low', 'Nariman Point Main Branch'),
('CUST-LRG-1002', 'Priya Patel', 'Retail', 'KYC Tier 1', 'Low', 'Bandra West Branch'),
('CUST-LRG-1003', 'Tata Steel Supply Chain Ltd', 'Corporate', 'KYC Tier 2', 'Medium', 'Fort Commercial Branch'),
('CUST-LRG-1004', 'Reliance Retail Vendor Partners', 'SME', 'KYC Tier 2', 'Medium', 'Andheri MIDC Branch'),
('CUST-LRG-1005', 'Dr. Vikram Sarabhai Trust', 'Wealth', 'KYC Tier 1', 'Low', 'BKC Wealth Centre')
ON CONFLICT (customer_id) DO NOTHING;

INSERT INTO bank_large.accounts (account_id, customer_id, account_type, currency, available_balance, hold_amount, status)
VALUES
('ACC-LRG-5001', 'CUST-LRG-1001', 'Savings CASA', 'INR', 1450000.00, 0.00, 'Active'),
('ACC-LRG-5002', 'CUST-LRG-1002', 'Savings CASA', 'INR', 820000.00, 0.00, 'Active'),
('ACC-LRG-5003', 'CUST-LRG-1003', 'Corporate Current', 'INR', 85000000.00, 0.00, 'Active'),
('ACC-LRG-5004', 'CUST-LRG-1004', 'Current CASA', 'INR', 12400000.00, 0.00, 'Active'),
('ACC-LRG-5005', 'CUST-LRG-1005', 'Savings CASA', 'INR', 45000000.00, 0.00, 'Active')
ON CONFLICT (account_id) DO NOTHING;

INSERT INTO bank_large.deposits (deposit_id, customer_id, product_type, principal_amount, interest_rate_pct, start_date, maturity_date, maturity_amount, renewal_instruction)
VALUES
('DEP-LRG-7001', 'CUST-LRG-1001', 'Cumulative Fixed Deposit', 5000000.00, 7.10, '2026-01-15', '2027-01-15', 5365000.00, 'Auto-Renew Principal & Interest'),
('DEP-LRG-7002', 'CUST-LRG-1003', 'Corporate Bulk Term Deposit', 100000000.00, 7.45, '2026-03-01', '2026-09-01', 103725000.00, 'Payout to Current Account')
ON CONFLICT (deposit_id) DO NOTHING;

INSERT INTO bank_large.loans (loan_id, customer_id, loan_type, sanctioned_amount, principal_outstanding, interest_rate_pct, days_past_due, irac_classification, collateral_value, next_emi_due_date, monthly_emi)
VALUES
('LN-LRG-9001', 'CUST-LRG-1001', 'Home Loan', 7500000.00, 6840000.00, 8.50, 0, 'Standard', 12000000.00, '2026-11-05', 65000.00),
('LN-LRG-9002', 'CUST-LRG-1004', 'Working Capital Term Loan', 50000000.00, 42000000.00, 10.25, 0, 'Standard', 70000000.00, '2026-11-10', 480000.00)
ON CONFLICT (loan_id) DO NOTHING;

INSERT INTO bank_large.treasury_holdings (holding_id, isin, instrument_name, category, quantity, face_value_unit, book_value, market_value, encumbrance_status, maturity_date, coupon_or_yield_pct)
VALUES
('LOT-LRG-101', 'IN0020230085', '7.18% GS 2033 (10Y Benchmark)', 'G-Sec', 15000000, 100.00, 1500000000.00, 1512600000.00, 'Unencumbered', '2033-08-14', 7.18),
('LOT-LRG-102', 'IN0020220011', '7.26% GS 2032', 'G-Sec', 20000000, 100.00, 2000000000.00, 2018400000.00, 'Unencumbered', '2032-02-06', 7.26),
('LOT-LRG-103', 'IN002024X025', '91-Day Treasury Bill', 'T-Bill', 5000000, 100.00, 491750000.00, 492100000.00, 'Unencumbered', '2026-12-15', 6.72)
ON CONFLICT (holding_id) DO NOTHING;

INSERT INTO bank_large.ledger_entries (entry_id, transaction_ref, account_reference, counterparty_ref, debit_inr, credit_inr, narration, status)
VALUES
('LED-LRG-01', 'TXN-INIT-01', 'CASH_RESERVE', NULL, 42000000000.00, 0.00, 'Regulatory Capital & Cash Inflow', 'Posted'),
('LED-LRG-02', 'TXN-INIT-01', 'SHAREHOLDERS_EQUITY', NULL, 0.00, 42000000000.00, 'Regulatory Capital & Cash Inflow', 'Posted')
ON CONFLICT (entry_id) DO NOTHING;

-- 4. Seed Bank Distressed (Kalyan Industrial Credit Bank)
INSERT INTO bank_distressed.customers (customer_id, full_name, segment, kyc_status, risk_category, branch_name)
VALUES
('CUST-DST-1001', 'Deccan Logistics & Freight Ltd', 'Corporate', 'KYC Tier 2', 'High', 'Kalyan Industrial Gate Branch'),
('CUST-DST-1002', 'Kiran Infrastructure Pvt Ltd', 'Corporate', 'KYC Tier 2', 'High', 'Thane Commercial Branch')
ON CONFLICT (customer_id) DO NOTHING;

INSERT INTO bank_distressed.loans (loan_id, customer_id, loan_type, sanctioned_amount, principal_outstanding, interest_rate_pct, days_past_due, irac_classification, collateral_value, next_emi_due_date, monthly_emi)
VALUES
('LN-DST-9001', 'CUST-DST-1001', 'Commercial WC Loan', 180000000.00, 168000000.00, 13.50, 95, 'Sub-Standard NPA', 110000000.00, '2026-08-01', 1950000.00),
('LN-DST-9002', 'CUST-DST-1002', 'Project Finance Facility', 240000000.00, 235000000.00, 12.80, 68, 'SMA-2', 180000000.00, '2026-09-01', 2800000.00)
ON CONFLICT (loan_id) DO NOTHING;

-- 5. Seed LRO Decision Support Snapshots & Recommendations
INSERT INTO lro.liquidity_snapshots (snapshot_id, bank_id, cash_available_inr, liquid_assets_hqla_inr, funding_gap_1d_inr, funding_gap_30d_inr, educational_lcr_pct, educational_nsfr_pct, market_data_freshness)
VALUES
('SNAP-LRG-01', 'BANK-01-LRG', 42000000000.00, 145000000000.00, 1250000000.00, 8400000000.00, 138.40, 118.20, 'LIVE STREAM ACTIVE (50ms)'),
('SNAP-DST-01', 'BANK-04-DST', 3100000000.00, 8500000000.00, -1850000000.00, -4200000000.00, 82.50, 89.10, 'LIVE STREAM ACTIVE (50ms)')
ON CONFLICT (snapshot_id) DO NOTHING;

INSERT INTO lro.recommendations (recommendation_id, bank_id, asset_name, proposed_action, suggested_quantity, expected_gross_proceeds_inr, estimated_cost_inr, net_proceeds_inr, lcr_benefit_pct, model_confidence_pct, rationale_text, status)
VALUES
('REC-2026-001', 'BANK-01-LRG', '7.18% GS 2033 (10Y Benchmark)', 'Bilateral Interbank Repo (3D)', 2500000, 252100000.00, 48500.00, 252051500.00, 4.25, 94.50, 'Repo borrowing against unencumbered sovereign lot resolves 1D funding gap with zero market price impact and minimal hair-cut.', 'Pending Review'),
('REC-2026-002', 'BANK-04-DST', '91-Day Treasury Bill', 'Outright Market Sale', 1500000, 147630000.00, 128400.00, 147501600.00, 8.10, 88.20, 'Urgent liquidity generation to restore minimum regulatory CRR and prevent 1-day clearing default under PCA stress.', 'Pending Review')
ON CONFLICT (recommendation_id) DO NOTHING;

INSERT INTO lro.risk_alerts (alert_id, bank_id, severity, title, description, acknowledged)
VALUES
('ALT-001', 'BANK-04-DST', 'CRITICAL', 'PCA Liquidity Buffer Breach Warning', 'Educational LCR fell to 82.50% (threshold 100.00%). Immediate liquidity replenishment required.', FALSE),
('ALT-002', 'BANK-04-DST', 'WARNING', '1D Contractual Net Funding Deficit', 'Projected net outflow deficit of ₹ 1,850.00 Cr in next 24 hours.', FALSE),
('ALT-003', 'BANK-01-LRG', 'INFO', 'SLR Sovereign Buffer Comfortable', 'Holding excess unencumbered G-Secs exceeding mandatory regulatory buffer by ₹ 8,400 Cr.', TRUE)
ON CONFLICT (alert_id) DO NOTHING;

INSERT INTO lro.audit_log (audit_id, actor_user_id, role_at_execution, action_name, target_entity_type, target_entity_id, details_json, signature_hash)
VALUES
('AUD-001', 'SYSTEM-ORCHESTRATOR', 'MAKER', 'INITIALIZE_TOPOLOGY', 'SYSTEM', 'POSTGRESQL-18', '{"db":"lro_db","server":"PostgreSQL 18","status":"ACTIVE"}', 'SHA256:7B8F9A1E02C3D4B5')
ON CONFLICT (audit_id) DO NOTHING;
