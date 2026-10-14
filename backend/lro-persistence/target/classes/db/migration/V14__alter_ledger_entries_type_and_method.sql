-- ==============================================================================
-- V14: Alter ledger_entries to rename narration to type and add method column
-- Type: Deposits, Withdrawal, Transfers, Payments, Investment
-- Method: UPI, RTGS, IMPS, NEFT, Cheque, Cash
-- ==============================================================================

-- 1. Update CDC trigger function first so it handles both narration and type/method safely
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

-- 2. Alter tables and randomize Type and Method across 4 bank schemas
DO $$
DECLARE
    sch text;
BEGIN
    FOR sch IN SELECT unnest(ARRAY['bank_large', 'bank_medium', 'bank_small', 'bank_distressed'])
    LOOP
        -- Rename narration to type if it exists
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = sch AND table_name = 'ledger_entries' AND column_name = 'narration') THEN
            EXECUTE format('ALTER TABLE %I.ledger_entries RENAME COLUMN narration TO type;', sch);
            EXECUTE format('ALTER TABLE %I.ledger_entries ALTER COLUMN type TYPE VARCHAR(64);', sch);
        END IF;

        -- Add method column and ensure column ordering: type | method | status
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = sch AND table_name = 'ledger_entries' AND column_name = 'method') THEN
            EXECUTE format('ALTER TABLE %I.ledger_entries ADD COLUMN method VARCHAR(32);', sch);
            EXECUTE format('ALTER TABLE %I.ledger_entries DROP COLUMN IF EXISTS status;', sch);
            EXECUTE format('ALTER TABLE %I.ledger_entries ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT ''Posted'';', sch);
        END IF;

        -- Update existing records with randomized Type and matching Method
        EXECUTE format($u$
            UPDATE %I.ledger_entries
            SET type = sub.t,
                method = CASE sub.t
                    WHEN 'Deposits' THEN (ARRAY['Cash', 'Cheque', 'NEFT', 'RTGS', 'IMPS', 'UPI'])[floor(random()*6 + 1)]
                    WHEN 'Withdrawal' THEN (ARRAY['Cash', 'UPI', 'Cheque', 'IMPS'])[floor(random()*4 + 1)]
                    WHEN 'Transfers' THEN (ARRAY['IMPS', 'NEFT', 'RTGS', 'UPI'])[floor(random()*4 + 1)]
                    WHEN 'Payments' THEN (ARRAY['UPI', 'IMPS', 'NEFT', 'Cheque', 'Cash'])[floor(random()*5 + 1)]
                    WHEN 'Investment' THEN (ARRAY['RTGS', 'NEFT', 'Cheque', 'IMPS'])[floor(random()*4 + 1)]
                    ELSE 'UPI'
                END
            FROM (
                SELECT entry_id, (ARRAY['Deposits', 'Withdrawal', 'Transfers', 'Payments', 'Investment'])[floor(random()*5 + 1)] AS t
                FROM %I.ledger_entries
            ) sub
            WHERE %I.ledger_entries.entry_id = sub.entry_id;
        $u$, sch, sch, sch);

        -- Enforce NOT NULL constraints
        EXECUTE format('ALTER TABLE %I.ledger_entries ALTER COLUMN type SET NOT NULL;', sch);
        EXECUTE format('ALTER TABLE %I.ledger_entries ALTER COLUMN method SET NOT NULL;', sch);
    END LOOP;
END $$;
