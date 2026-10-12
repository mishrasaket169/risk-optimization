-- V12: Add bidirectional Change Data Capture (CDC) triggers for PostgreSQL LISTEN/NOTIFY
-- Enables instant bidirectional real-time event streaming to the Java application at every instance

CREATE OR REPLACE FUNCTION notify_bank_event() RETURNS trigger AS $$
DECLARE
    payload jsonb;
    rec_id text;
    cust_id text;
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
        payload = payload || jsonb_build_object(
            'entry_id', NEW.entry_id,
            'ref', NEW.transaction_ref,
            'acc_ref', NEW.account_reference,
            'debit', NEW.debit_inr,
            'credit', NEW.credit_inr,
            'narration', NEW.narration
        );
    ELSIF (TG_TABLE_NAME = 'treasury_holdings') THEN
        payload = payload || jsonb_build_object(
            'holding_id', NEW.holding_id,
            'market_value', NEW.market_value,
            'category', NEW.category
        );
    END IF;

    -- Send notification on channel 'bank_events'
    PERFORM pg_notify('bank_events', payload::text);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply triggers across all 4 bank schemas
DO $$
DECLARE
    sch text;
BEGIN
    FOR sch IN SELECT unnest(ARRAY['bank_large', 'bank_medium', 'bank_small', 'bank_distressed'])
    LOOP
        -- 1. Accounts trigger
        EXECUTE format('DROP TRIGGER IF EXISTS trg_%I_accounts_cdc ON %I.accounts;', sch, sch);
        EXECUTE format('CREATE TRIGGER trg_%I_accounts_cdc AFTER INSERT OR UPDATE ON %I.accounts FOR EACH ROW EXECUTE FUNCTION notify_bank_event();', sch, sch);

        -- 2. Loans trigger
        EXECUTE format('DROP TRIGGER IF EXISTS trg_%I_loans_cdc ON %I.loans;', sch, sch);
        EXECUTE format('CREATE TRIGGER trg_%I_loans_cdc AFTER INSERT OR UPDATE ON %I.loans FOR EACH ROW EXECUTE FUNCTION notify_bank_event();', sch, sch);

        -- 3. Ledger trigger
        EXECUTE format('DROP TRIGGER IF EXISTS trg_%I_ledger_cdc ON %I.ledger_entries;', sch, sch);
        EXECUTE format('CREATE TRIGGER trg_%I_ledger_cdc AFTER INSERT OR UPDATE ON %I.ledger_entries FOR EACH ROW EXECUTE FUNCTION notify_bank_event();', sch, sch);

        -- 4. Treasury holdings trigger
        EXECUTE format('DROP TRIGGER IF EXISTS trg_%I_treasury_cdc ON %I.treasury_holdings;', sch, sch);
        EXECUTE format('CREATE TRIGGER trg_%I_treasury_cdc AFTER INSERT OR UPDATE ON %I.treasury_holdings FOR EACH ROW EXECUTE FUNCTION notify_bank_event();', sch, sch);
    END LOOP;
END $$;
