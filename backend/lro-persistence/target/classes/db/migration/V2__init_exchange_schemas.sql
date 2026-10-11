-- V2: Normalized Exchange Simulator Schema

CREATE SCHEMA IF NOT EXISTS exchange;

CREATE TABLE IF NOT EXISTS exchange.instruments (
    symbol VARCHAR(32) PRIMARY KEY,
    isin VARCHAR(32) NOT NULL,
    name VARCHAR(128) NOT NULL,
    category VARCHAR(32) NOT NULL,
    sector VARCHAR(64),
    tick_size NUMERIC(10, 4) NOT NULL DEFAULT 0.05,
    lot_size INT NOT NULL DEFAULT 1,
    last_traded_price NUMERIC(18, 4) NOT NULL,
    day_change_pct NUMERIC(8, 4) NOT NULL DEFAULT 0.00,
    bid_price NUMERIC(18, 4) NOT NULL,
    ask_price NUMERIC(18, 4) NOT NULL,
    day_volume BIGINT NOT NULL DEFAULT 0,
    day_turnover_inr NUMERIC(18, 2) NOT NULL DEFAULT 0.00,
    coupon_or_yield_pct NUMERIC(6, 2),
    maturity_or_expiry DATE,
    strike_price NUMERIC(18, 2),
    option_type VARCHAR(4),
    last_quote_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS exchange.orders (
    order_id VARCHAR(64) PRIMARY KEY,
    client_ref_id VARCHAR(64) NOT NULL,
    placed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    symbol VARCHAR(32) REFERENCES exchange.instruments(symbol),
    side VARCHAR(8) NOT NULL,
    order_type VARCHAR(16) NOT NULL,
    quantity BIGINT NOT NULL,
    filled_quantity BIGINT NOT NULL DEFAULT 0,
    limit_price NUMERIC(18, 4),
    average_fill_price NUMERIC(18, 4) DEFAULT 0.00,
    status VARCHAR(24) NOT NULL DEFAULT 'Accepted',
    time_in_force VARCHAR(8) NOT NULL DEFAULT 'DAY',
    estimated_notional_inr NUMERIC(18, 2) NOT NULL
);

CREATE TABLE IF NOT EXISTS exchange.trades (
    trade_id VARCHAR(64) PRIMARY KEY,
    order_id VARCHAR(64) REFERENCES exchange.orders(order_id),
    executed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    symbol VARCHAR(32) NOT NULL,
    side VARCHAR(8) NOT NULL,
    quantity BIGINT NOT NULL,
    price NUMERIC(18, 4) NOT NULL,
    gross_value_inr NUMERIC(18, 2) NOT NULL
);
