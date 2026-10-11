-- V1: Normalized Banking Schemas for the 4 fictional institutions
-- (bank_large, bank_medium, bank_small, bank_distressed)

CREATE SCHEMA IF NOT EXISTS bank_large;
CREATE SCHEMA IF NOT EXISTS bank_medium;
CREATE SCHEMA IF NOT EXISTS bank_small;
CREATE SCHEMA IF NOT EXISTS bank_distressed;

-- Macro function to create standard tables in any bank schema
CREATE TABLE IF NOT EXISTS bank_large.customers (
    customer_id VARCHAR(64) PRIMARY KEY,
    full_name VARCHAR(128) NOT NULL,
    segment VARCHAR(32) NOT NULL,
    kyc_status VARCHAR(32) NOT NULL,
    risk_category VARCHAR(16) NOT NULL,
    branch_name VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS bank_large.accounts (
    account_id VARCHAR(64) PRIMARY KEY,
    customer_id VARCHAR(64) REFERENCES bank_large.customers(customer_id),
    account_type VARCHAR(32) NOT NULL,
    currency VARCHAR(8) DEFAULT 'INR',
    available_balance NUMERIC(18, 2) NOT NULL DEFAULT 0.00,
    hold_amount NUMERIC(18, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(16) NOT NULL DEFAULT 'Active',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS bank_large.deposits (
    deposit_id VARCHAR(64) PRIMARY KEY,
    customer_id VARCHAR(64) REFERENCES bank_large.customers(customer_id),
    product_type VARCHAR(32) NOT NULL,
    principal_amount NUMERIC(18, 2) NOT NULL,
    interest_rate_pct NUMERIC(6, 2) NOT NULL,
    start_date DATE NOT NULL,
    maturity_date DATE NOT NULL,
    maturity_amount NUMERIC(18, 2) NOT NULL,
    renewal_instruction VARCHAR(64) NOT NULL
);

CREATE TABLE IF NOT EXISTS bank_large.loans (
    loan_id VARCHAR(64) PRIMARY KEY,
    customer_id VARCHAR(64) REFERENCES bank_large.customers(customer_id),
    loan_type VARCHAR(32) NOT NULL,
    sanctioned_amount NUMERIC(18, 2) NOT NULL,
    principal_outstanding NUMERIC(18, 2) NOT NULL,
    interest_rate_pct NUMERIC(6, 2) NOT NULL,
    days_past_due INT NOT NULL DEFAULT 0,
    irac_classification VARCHAR(32) NOT NULL DEFAULT 'Standard',
    collateral_value NUMERIC(18, 2) NOT NULL DEFAULT 0.00,
    next_emi_due_date DATE NOT NULL,
    monthly_emi NUMERIC(18, 2) NOT NULL
);

CREATE TABLE IF NOT EXISTS bank_large.treasury_holdings (
    holding_id VARCHAR(64) PRIMARY KEY,
    isin VARCHAR(32) NOT NULL,
    instrument_name VARCHAR(128) NOT NULL,
    category VARCHAR(32) NOT NULL,
    quantity BIGINT NOT NULL,
    face_value_unit NUMERIC(18, 2) NOT NULL,
    book_value NUMERIC(18, 2) NOT NULL,
    market_value NUMERIC(18, 2) NOT NULL,
    encumbrance_status VARCHAR(32) NOT NULL DEFAULT 'Unencumbered',
    maturity_date DATE,
    coupon_or_yield_pct NUMERIC(6, 2) NOT NULL DEFAULT 0.00,
    last_valuation_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS bank_large.ledger_entries (
    entry_id VARCHAR(64) PRIMARY KEY,
    transaction_ref VARCHAR(64) NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    account_reference VARCHAR(64) NOT NULL,
    counterparty_ref VARCHAR(64),
    debit_inr NUMERIC(18, 2) NOT NULL DEFAULT 0.00,
    credit_inr NUMERIC(18, 2) NOT NULL DEFAULT 0.00,
    narration VARCHAR(255) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'Posted'
);

-- Replicate normalized tables for medium, small, distressed banks
CREATE TABLE IF NOT EXISTS bank_medium.customers AS SELECT * FROM bank_large.customers WITH NO DATA;
CREATE TABLE IF NOT EXISTS bank_medium.accounts AS SELECT * FROM bank_large.accounts WITH NO DATA;
CREATE TABLE IF NOT EXISTS bank_medium.deposits AS SELECT * FROM bank_large.deposits WITH NO DATA;
CREATE TABLE IF NOT EXISTS bank_medium.loans AS SELECT * FROM bank_large.loans WITH NO DATA;
CREATE TABLE IF NOT EXISTS bank_medium.treasury_holdings AS SELECT * FROM bank_large.treasury_holdings WITH NO DATA;
CREATE TABLE IF NOT EXISTS bank_medium.ledger_entries AS SELECT * FROM bank_large.ledger_entries WITH NO DATA;

CREATE TABLE IF NOT EXISTS bank_small.customers AS SELECT * FROM bank_large.customers WITH NO DATA;
CREATE TABLE IF NOT EXISTS bank_small.accounts AS SELECT * FROM bank_large.accounts WITH NO DATA;
CREATE TABLE IF NOT EXISTS bank_small.deposits AS SELECT * FROM bank_large.deposits WITH NO DATA;
CREATE TABLE IF NOT EXISTS bank_small.loans AS SELECT * FROM bank_large.loans WITH NO DATA;
CREATE TABLE IF NOT EXISTS bank_small.treasury_holdings AS SELECT * FROM bank_large.treasury_holdings WITH NO DATA;
CREATE TABLE IF NOT EXISTS bank_small.ledger_entries AS SELECT * FROM bank_large.ledger_entries WITH NO DATA;

CREATE TABLE IF NOT EXISTS bank_distressed.customers AS SELECT * FROM bank_large.customers WITH NO DATA;
CREATE TABLE IF NOT EXISTS bank_distressed.accounts AS SELECT * FROM bank_large.accounts WITH NO DATA;
CREATE TABLE IF NOT EXISTS bank_distressed.deposits AS SELECT * FROM bank_large.deposits WITH NO DATA;
CREATE TABLE IF NOT EXISTS bank_distressed.loans AS SELECT * FROM bank_large.loans WITH NO DATA;
CREATE TABLE IF NOT EXISTS bank_distressed.treasury_holdings AS SELECT * FROM bank_large.treasury_holdings WITH NO DATA;
CREATE TABLE IF NOT EXISTS bank_distressed.ledger_entries AS SELECT * FROM bank_large.ledger_entries WITH NO DATA;
