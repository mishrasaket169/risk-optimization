-- V3: Normalized LRO Decision Support & Audit Schema

CREATE SCHEMA IF NOT EXISTS lro;

CREATE TABLE IF NOT EXISTS lro.liquidity_snapshots (
    snapshot_id VARCHAR(64) PRIMARY KEY,
    bank_id VARCHAR(32) NOT NULL,
    as_of_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    cash_available_inr NUMERIC(18, 2) NOT NULL,
    liquid_assets_hqla_inr NUMERIC(18, 2) NOT NULL,
    funding_gap_1d_inr NUMERIC(18, 2) NOT NULL,
    funding_gap_30d_inr NUMERIC(18, 2) NOT NULL,
    educational_lcr_pct NUMERIC(8, 2) NOT NULL,
    educational_nsfr_pct NUMERIC(8, 2) NOT NULL,
    market_data_freshness VARCHAR(64) NOT NULL
);

CREATE TABLE IF NOT EXISTS lro.recommendations (
    recommendation_id VARCHAR(64) PRIMARY KEY,
    bank_id VARCHAR(32) NOT NULL,
    asset_name VARCHAR(128) NOT NULL,
    proposed_action VARCHAR(64) NOT NULL,
    suggested_quantity BIGINT NOT NULL,
    expected_gross_proceeds_inr NUMERIC(18, 2) NOT NULL,
    estimated_cost_inr NUMERIC(18, 2) NOT NULL,
    net_proceeds_inr NUMERIC(18, 2) NOT NULL,
    lcr_benefit_pct NUMERIC(6, 2) NOT NULL,
    model_confidence_pct NUMERIC(6, 2) NOT NULL,
    rationale_text TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'Pending Review',
    generated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS lro.risk_alerts (
    alert_id VARCHAR(64) PRIMARY KEY,
    bank_id VARCHAR(32) NOT NULL,
    severity VARCHAR(16) NOT NULL,
    title VARCHAR(128) NOT NULL,
    description TEXT NOT NULL,
    triggered_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    acknowledged BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS lro.audit_log (
    audit_id VARCHAR(64) PRIMARY KEY,
    actor_user_id VARCHAR(64) NOT NULL,
    role_at_execution VARCHAR(64) NOT NULL,
    action_name VARCHAR(64) NOT NULL,
    target_entity_type VARCHAR(64) NOT NULL,
    target_entity_id VARCHAR(64) NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    details_json TEXT NOT NULL,
    signature_hash VARCHAR(64) NOT NULL
);
