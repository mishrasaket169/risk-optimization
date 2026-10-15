-- ==============================================================================
-- V16: Implement Balance Sheet tables and seed 2-year daily transaction history
-- Counts:
--   bank_large:      1,000,000 transactions
--   bank_medium:       500,000 transactions
--   bank_small:        300,000 transactions
--   bank_distressed:   180,000 transactions
-- Total:             1,980,000 transactions
-- ==============================================================================

-- 1. Update CDC trigger function to support balance_sheet notifications
CREATE OR REPLACE FUNCTION notify_bank_event()
RETURNS TRIGGER AS $$
DECLARE
    payload jsonb;
    t_val text;
    m_val text;
BEGIN
    payload = jsonb_build_object(
        'schema', TG_TABLE_SCHEMA,
        'table', TG_TABLE_NAME,
        'action', TG_OP,
        'ts', CURRENT_TIMESTAMP
    );

    IF (TG_TABLE_NAME = 'accounts') THEN
        payload = payload || jsonb_build_object(
            'account_id', NEW.account_id,
            'customer_id', NEW.customer_id,
            'balance', NEW.available_balance,
            'status', NEW.status
        );
    ELSIF (TG_TABLE_NAME = 'loans') THEN
        payload = payload || jsonb_build_object(
            'loan_id', NEW.loan_id,
            'customer_id', NEW.customer_id,
            'principal', NEW.principal_outstanding,
            'irac', NEW.irac_classification,
            'dpd', NEW.days_past_due
        );
    ELSIF (TG_TABLE_NAME = 'ledger_entries') THEN
        BEGIN
            t_val := to_jsonb(NEW)->>'type';
            m_val := to_jsonb(NEW)->>'method';
        EXCEPTION WHEN OTHERS THEN
            t_val := 'Deposits';
            m_val := 'UPI';
        END;

        payload = payload || jsonb_build_object(
            'entry_id', NEW.entry_id,
            'ref', NEW.transaction_ref,
            'acc_ref', NEW.account_reference,
            'debit', NEW.debit_inr,
            'credit', NEW.credit_inr,
            'type', t_val,
            'method', m_val
        );
    ELSIF (TG_TABLE_NAME = 'balance_sheet') THEN
        payload = payload || jsonb_build_object(
            'record_id', NEW.record_id,
            'acc_ref', NEW.account_reference,
            'line_item', NEW.line_item,
            'category', NEW.category,
            'debit', NEW.debit_inr,
            'credit', NEW.credit_inr,
            'balance', NEW.running_balance_inr,
            'type', NEW.type,
            'method', NEW.method
        );
    ELSIF (TG_TABLE_NAME = 'treasury_holdings') THEN
        payload = payload || jsonb_build_object(
            'holding_id', NEW.holding_id,
            'market_value', NEW.market_value,
            'category', NEW.category
        );
    END IF;

    PERFORM pg_notify('bank_events', payload::text);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 2. Create balance_sheet tables in all 4 bank schemas
DO $$
DECLARE
    sch text;
BEGIN
    FOR sch IN SELECT unnest(ARRAY['bank_large', 'bank_medium', 'bank_small', 'bank_distressed'])
    LOOP
        EXECUTE format('
            CREATE TABLE IF NOT EXISTS %I.balance_sheet (
                record_id VARCHAR(64) PRIMARY KEY,
                timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
                account_reference VARCHAR(64) NOT NULL,
                line_item VARCHAR(64) NOT NULL,
                category VARCHAR(32) NOT NULL,
                debit_inr NUMERIC(18, 2) NOT NULL DEFAULT 0.00,
                credit_inr NUMERIC(18, 2) NOT NULL DEFAULT 0.00,
                running_balance_inr NUMERIC(18, 2) NOT NULL DEFAULT 0.00,
                type VARCHAR(64) NOT NULL,
                method VARCHAR(32) NOT NULL,
                status VARCHAR(16) NOT NULL DEFAULT ''Posted''
            );
        ', sch);

        -- Ensure any existing triggers are dropped during bulk insert
        EXECUTE format('DROP TRIGGER IF EXISTS trg_balance_sheet_notify ON %I.balance_sheet;', sch);
    END LOOP;
END $$;

-- Truncate any previous partial data
TRUNCATE TABLE bank_large.balance_sheet;
TRUNCATE TABLE bank_medium.balance_sheet;
TRUNCATE TABLE bank_small.balance_sheet;
TRUNCATE TABLE bank_distressed.balance_sheet;

-- 3. Fast Bulk Seed historical 2-year daily transactions
-- bank_large: 1,000,000 rows
INSERT INTO bank_large.balance_sheet (
    record_id, timestamp, account_reference, line_item, category,
    debit_inr, credit_inr, running_balance_inr, type, method, status
)
SELECT
    'BS-LRG-' || to_char(i, 'FM0000000'),
    CURRENT_TIMESTAMP - (((1000000::bigint - i)::double precision / 1000000.0 * 730.0) || ' days')::interval + (((i * 37::bigint) % 86400::bigint) || ' seconds')::interval,
    'ACC-LRG-' || to_char(((i * 73::bigint) % 25000::bigint) + 1, 'FM00000'),
    CASE (i % 10::bigint)
        WHEN 0 THEN 'Loans & Advances'
        WHEN 1 THEN 'Loans & Advances'
        WHEN 2 THEN 'Treasury & G-Sec Investments'
        WHEN 3 THEN 'Treasury & G-Sec Investments'
        WHEN 4 THEN 'Cash & RBI Reserves'
        WHEN 5 THEN 'CASA Deposits'
        WHEN 6 THEN 'CASA Deposits'
        WHEN 7 THEN 'Term & Fixed Deposits'
        WHEN 8 THEN 'Interbank Borrowings & Repo'
        ELSE 'Statutory Reserves & Retained Surplus'
    END,
    CASE (i % 10::bigint)
        WHEN 0 THEN 'Asset'
        WHEN 1 THEN 'Asset'
        WHEN 2 THEN 'Asset'
        WHEN 3 THEN 'Asset'
        WHEN 4 THEN 'Asset'
        WHEN 5 THEN 'Liability'
        WHEN 6 THEN 'Liability'
        WHEN 7 THEN 'Liability'
        WHEN 8 THEN 'Liability'
        ELSE 'Equity'
    END,
    CASE (i % 10::bigint)
        WHEN 0 THEN round((15000::bigint + ((i * 1337::bigint) % 3500000::bigint))::numeric, 2)
        WHEN 2 THEN round((75000::bigint + ((i * 3141::bigint) % 6000000::bigint))::numeric, 2)
        WHEN 4 THEN round((10000::bigint + ((i * 2718::bigint) % 800000::bigint))::numeric, 2)
        WHEN 6 THEN round((1500::bigint + ((i * 9973::bigint) % 150000::bigint))::numeric, 2)
        ELSE 0.00
    END,
    CASE (i % 10::bigint)
        WHEN 1 THEN round((8000::bigint + ((i * 1337::bigint) % 450000::bigint))::numeric, 2)
        WHEN 3 THEN round((75000::bigint + ((i * 3141::bigint) % 6000000::bigint))::numeric, 2)
        WHEN 5 THEN round((1500::bigint + ((i * 9973::bigint) % 300000::bigint))::numeric, 2)
        WHEN 7 THEN round((80000::bigint + ((i * 1234::bigint) % 1500000::bigint))::numeric, 2)
        WHEN 8 THEN round((150000::bigint + ((i * 5678::bigint) % 4000000::bigint))::numeric, 2)
        WHEN 9 THEN round((20000::bigint + ((i * 4321::bigint) % 800000::bigint))::numeric, 2)
        ELSE 0.00
    END,
    round((1850000000000.00 + (i::numeric / 1000000.0 * 200000000000.00) + (((i * 17::bigint) % 500::bigint) - 250::bigint)::numeric * 2000000.00)::numeric, 2),
    CASE (i % 10::bigint)
        WHEN 0 THEN 'Loan Disbursement'
        WHEN 1 THEN 'EMI Repayment'
        WHEN 2 THEN 'Investment'
        WHEN 3 THEN 'Treasury Settlement'
        WHEN 4 THEN 'Transfers'
        WHEN 5 THEN 'Deposits'
        WHEN 6 THEN 'Withdrawal'
        WHEN 7 THEN 'Deposits'
        WHEN 8 THEN 'Interbank Borrowing'
        ELSE 'Reserve Adjustment'
    END,
    CASE (i % 6::bigint)
        WHEN 0 THEN 'UPI'
        WHEN 1 THEN 'RTGS'
        WHEN 2 THEN 'NEFT'
        WHEN 3 THEN 'IMPS'
        WHEN 4 THEN 'Cheque'
        ELSE 'Cash'
    END,
    'Posted'
FROM generate_series(1::bigint, 1000000::bigint) i;

-- bank_medium: 500,000 rows
INSERT INTO bank_medium.balance_sheet (
    record_id, timestamp, account_reference, line_item, category,
    debit_inr, credit_inr, running_balance_inr, type, method, status
)
SELECT
    'BS-MED-' || to_char(i, 'FM0000000'),
    CURRENT_TIMESTAMP - (((500000::bigint - i)::double precision / 500000.0 * 730.0) || ' days')::interval + (((i * 41::bigint) % 86400::bigint) || ' seconds')::interval,
    'ACC-MED-' || to_char(((i * 73::bigint) % 15000::bigint) + 1, 'FM00000'),
    CASE (i % 10::bigint)
        WHEN 0 THEN 'Loans & Advances'
        WHEN 1 THEN 'Loans & Advances'
        WHEN 2 THEN 'Treasury & G-Sec Investments'
        WHEN 3 THEN 'Treasury & G-Sec Investments'
        WHEN 4 THEN 'Cash & RBI Reserves'
        WHEN 5 THEN 'CASA Deposits'
        WHEN 6 THEN 'CASA Deposits'
        WHEN 7 THEN 'Term & Fixed Deposits'
        WHEN 8 THEN 'Interbank Borrowings & Repo'
        ELSE 'Statutory Reserves & Retained Surplus'
    END,
    CASE (i % 10::bigint)
        WHEN 0 THEN 'Asset'
        WHEN 1 THEN 'Asset'
        WHEN 2 THEN 'Asset'
        WHEN 3 THEN 'Asset'
        WHEN 4 THEN 'Asset'
        WHEN 5 THEN 'Liability'
        WHEN 6 THEN 'Liability'
        WHEN 7 THEN 'Liability'
        WHEN 8 THEN 'Liability'
        ELSE 'Equity'
    END,
    CASE (i % 10::bigint)
        WHEN 0 THEN round((10000::bigint + ((i * 1337::bigint) % 2000000::bigint))::numeric, 2)
        WHEN 2 THEN round((50000::bigint + ((i * 3141::bigint) % 4000000::bigint))::numeric, 2)
        WHEN 4 THEN round((8000::bigint + ((i * 2718::bigint) % 500000::bigint))::numeric, 2)
        WHEN 6 THEN round((1000::bigint + ((i * 9973::bigint) % 100000::bigint))::numeric, 2)
        ELSE 0.00
    END,
    CASE (i % 10::bigint)
        WHEN 1 THEN round((6000::bigint + ((i * 1337::bigint) % 300000::bigint))::numeric, 2)
        WHEN 3 THEN round((50000::bigint + ((i * 3141::bigint) % 4000000::bigint))::numeric, 2)
        WHEN 5 THEN round((1000::bigint + ((i * 9973::bigint) % 200000::bigint))::numeric, 2)
        WHEN 7 THEN round((60000::bigint + ((i * 1234::bigint) % 1000000::bigint))::numeric, 2)
        WHEN 8 THEN round((100000::bigint + ((i * 5678::bigint) % 2500000::bigint))::numeric, 2)
        WHEN 9 THEN round((15000::bigint + ((i * 4321::bigint) % 500000::bigint))::numeric, 2)
        ELSE 0.00
    END,
    round((750000000000.00 + (i::numeric / 500000.0 * 100000000000.00) + (((i * 19::bigint) % 500::bigint) - 250::bigint)::numeric * 1500000.00)::numeric, 2),
    CASE (i % 10::bigint)
        WHEN 0 THEN 'Loan Disbursement'
        WHEN 1 THEN 'EMI Repayment'
        WHEN 2 THEN 'Investment'
        WHEN 3 THEN 'Treasury Settlement'
        WHEN 4 THEN 'Transfers'
        WHEN 5 THEN 'Deposits'
        WHEN 6 THEN 'Withdrawal'
        WHEN 7 THEN 'Deposits'
        WHEN 8 THEN 'Interbank Borrowing'
        ELSE 'Reserve Adjustment'
    END,
    CASE (i % 6::bigint)
        WHEN 0 THEN 'UPI'
        WHEN 1 THEN 'RTGS'
        WHEN 2 THEN 'NEFT'
        WHEN 3 THEN 'IMPS'
        WHEN 4 THEN 'Cheque'
        ELSE 'Cash'
    END,
    'Posted'
FROM generate_series(1::bigint, 500000::bigint) i;

-- bank_small: 300,000 rows
INSERT INTO bank_small.balance_sheet (
    record_id, timestamp, account_reference, line_item, category,
    debit_inr, credit_inr, running_balance_inr, type, method, status
)
SELECT
    'BS-SML-' || to_char(i, 'FM0000000'),
    CURRENT_TIMESTAMP - (((300000::bigint - i)::double precision / 300000.0 * 730.0) || ' days')::interval + (((i * 43::bigint) % 86400::bigint) || ' seconds')::interval,
    'ACC-SML-' || to_char(((i * 73::bigint) % 12000::bigint) + 1, 'FM00000'),
    CASE (i % 10::bigint)
        WHEN 0 THEN 'Loans & Advances'
        WHEN 1 THEN 'Loans & Advances'
        WHEN 2 THEN 'Treasury & G-Sec Investments'
        WHEN 3 THEN 'Treasury & G-Sec Investments'
        WHEN 4 THEN 'Cash & RBI Reserves'
        WHEN 5 THEN 'CASA Deposits'
        WHEN 6 THEN 'CASA Deposits'
        WHEN 7 THEN 'Term & Fixed Deposits'
        WHEN 8 THEN 'Interbank Borrowings & Repo'
        ELSE 'Statutory Reserves & Retained Surplus'
    END,
    CASE (i % 10::bigint)
        WHEN 0 THEN 'Asset'
        WHEN 1 THEN 'Asset'
        WHEN 2 THEN 'Asset'
        WHEN 3 THEN 'Asset'
        WHEN 4 THEN 'Asset'
        WHEN 5 THEN 'Liability'
        WHEN 6 THEN 'Liability'
        WHEN 7 THEN 'Liability'
        WHEN 8 THEN 'Liability'
        ELSE 'Equity'
    END,
    CASE (i % 10::bigint)
        WHEN 0 THEN round((8000::bigint + ((i * 1337::bigint) % 1200000::bigint))::numeric, 2)
        WHEN 2 THEN round((30000::bigint + ((i * 3141::bigint) % 2500000::bigint))::numeric, 2)
        WHEN 4 THEN round((5000::bigint + ((i * 2718::bigint) % 300000::bigint))::numeric, 2)
        WHEN 6 THEN round((800::bigint + ((i * 9973::bigint) % 80000::bigint))::numeric, 2)
        ELSE 0.00
    END,
    CASE (i % 10::bigint)
        WHEN 1 THEN round((4000::bigint + ((i * 1337::bigint) % 200000::bigint))::numeric, 2)
        WHEN 3 THEN round((30000::bigint + ((i * 3141::bigint) % 2500000::bigint))::numeric, 2)
        WHEN 5 THEN round((800::bigint + ((i * 9973::bigint) % 150000::bigint))::numeric, 2)
        WHEN 7 THEN round((40000::bigint + ((i * 1234::bigint) % 600000::bigint))::numeric, 2)
        WHEN 8 THEN round((60000::bigint + ((i * 5678::bigint) % 1500000::bigint))::numeric, 2)
        WHEN 9 THEN round((10000::bigint + ((i * 4321::bigint) % 300000::bigint))::numeric, 2)
        ELSE 0.00
    END,
    round((380000000000.00 + (i::numeric / 300000.0 * 50000000000.00) + (((i * 23::bigint) % 400::bigint) - 200::bigint)::numeric * 1000000.00)::numeric, 2),
    CASE (i % 10::bigint)
        WHEN 0 THEN 'Loan Disbursement'
        WHEN 1 THEN 'EMI Repayment'
        WHEN 2 THEN 'Investment'
        WHEN 3 THEN 'Treasury Settlement'
        WHEN 4 THEN 'Transfers'
        WHEN 5 THEN 'Deposits'
        WHEN 6 THEN 'Withdrawal'
        WHEN 7 THEN 'Deposits'
        WHEN 8 THEN 'Interbank Borrowing'
        ELSE 'Reserve Adjustment'
    END,
    CASE (i % 6::bigint)
        WHEN 0 THEN 'UPI'
        WHEN 1 THEN 'RTGS'
        WHEN 2 THEN 'NEFT'
        WHEN 3 THEN 'IMPS'
        WHEN 4 THEN 'Cheque'
        ELSE 'Cash'
    END,
    'Posted'
FROM generate_series(1::bigint, 300000::bigint) i;

-- bank_distressed: 180,000 rows
INSERT INTO bank_distressed.balance_sheet (
    record_id, timestamp, account_reference, line_item, category,
    debit_inr, credit_inr, running_balance_inr, type, method, status
)
SELECT
    'BS-DST-' || to_char(i, 'FM0000000'),
    CURRENT_TIMESTAMP - (((180000::bigint - i)::double precision / 180000.0 * 730.0) || ' days')::interval + (((i * 47::bigint) % 86400::bigint) || ' seconds')::interval,
    'ACC-DST-' || to_char(((i * 73::bigint) % 8000::bigint) + 1, 'FM00000'),
    CASE (i % 10::bigint)
        WHEN 0 THEN 'Loans & Advances'
        WHEN 1 THEN 'Loans & Advances'
        WHEN 2 THEN 'Treasury & G-Sec Investments'
        WHEN 3 THEN 'Treasury & G-Sec Investments'
        WHEN 4 THEN 'Cash & RBI Reserves'
        WHEN 5 THEN 'CASA Deposits'
        WHEN 6 THEN 'CASA Deposits'
        WHEN 7 THEN 'Term & Fixed Deposits'
        WHEN 8 THEN 'Interbank Borrowings & Repo'
        ELSE 'Statutory Reserves & Retained Surplus'
    END,
    CASE (i % 10::bigint)
        WHEN 0 THEN 'Asset'
        WHEN 1 THEN 'Asset'
        WHEN 2 THEN 'Asset'
        WHEN 3 THEN 'Asset'
        WHEN 4 THEN 'Asset'
        WHEN 5 THEN 'Liability'
        WHEN 6 THEN 'Liability'
        WHEN 7 THEN 'Liability'
        WHEN 8 THEN 'Liability'
        ELSE 'Equity'
    END,
    CASE (i % 10::bigint)
        WHEN 0 THEN round((5000::bigint + ((i * 1337::bigint) % 800000::bigint))::numeric, 2)
        WHEN 2 THEN round((20000::bigint + ((i * 3141::bigint) % 1500000::bigint))::numeric, 2)
        WHEN 4 THEN round((3000::bigint + ((i * 2718::bigint) % 200000::bigint))::numeric, 2)
        WHEN 6 THEN round((1200::bigint + ((i * 9973::bigint) % 120000::bigint))::numeric, 2)
        ELSE 0.00
    END,
    CASE (i % 10::bigint)
        WHEN 1 THEN round((3000::bigint + ((i * 1337::bigint) % 150000::bigint))::numeric, 2)
        WHEN 3 THEN round((20000::bigint + ((i * 3141::bigint) % 1500000::bigint))::numeric, 2)
        WHEN 5 THEN round((500::bigint + ((i * 9973::bigint) % 80000::bigint))::numeric, 2)
        WHEN 7 THEN round((25000::bigint + ((i * 1234::bigint) % 400000::bigint))::numeric, 2)
        WHEN 8 THEN round((40000::bigint + ((i * 5678::bigint) % 1000000::bigint))::numeric, 2)
        WHEN 9 THEN round((5000::bigint + ((i * 4321::bigint) % 150000::bigint))::numeric, 2)
        ELSE 0.00
    END,
    round((135000000000.00 - (i::numeric / 180000.0 * 10000000000.00) + (((i * 29::bigint) % 300::bigint) - 150::bigint)::numeric * 800000.00)::numeric, 2),
    CASE (i % 10::bigint)
        WHEN 0 THEN 'Loan Disbursement'
        WHEN 1 THEN 'EMI Repayment'
        WHEN 2 THEN 'Investment'
        WHEN 3 THEN 'Treasury Settlement'
        WHEN 4 THEN 'Transfers'
        WHEN 5 THEN 'Deposits'
        WHEN 6 THEN 'Withdrawal'
        WHEN 7 THEN 'Deposits'
        WHEN 8 THEN 'Interbank Borrowing'
        ELSE 'Reserve Adjustment'
    END,
    CASE (i % 6::bigint)
        WHEN 0 THEN 'UPI'
        WHEN 1 THEN 'RTGS'
        WHEN 2 THEN 'NEFT'
        WHEN 3 THEN 'IMPS'
        WHEN 4 THEN 'Cheque'
        ELSE 'Cash'
    END,
    'Posted'
FROM generate_series(1::bigint, 180000::bigint) i;

-- 4. Create Indexes and Triggers now that bulk seeding is complete
DO $$
DECLARE
    sch text;
BEGIN
    FOR sch IN SELECT unnest(ARRAY['bank_large', 'bank_medium', 'bank_small', 'bank_distressed'])
    LOOP
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%s_bs_timestamp ON %I.balance_sheet (timestamp DESC);', sch, sch);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%s_bs_category ON %I.balance_sheet (category);', sch, sch);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%s_bs_line_item ON %I.balance_sheet (line_item);', sch, sch);

        EXECUTE format('
            CREATE TRIGGER trg_balance_sheet_notify
            AFTER INSERT ON %I.balance_sheet
            FOR EACH ROW EXECUTE FUNCTION notify_bank_event();
        ', sch);
    END LOOP;
END $$;
