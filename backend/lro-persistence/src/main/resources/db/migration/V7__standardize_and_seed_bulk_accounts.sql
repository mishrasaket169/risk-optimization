-- V7: Standardize 7 account types and seed bulk accounts across all 4 bank schemas
-- Account types: Saving, Salary, Current, Fixed Deposit, Recurring Deposit, NRI, Demat
-- Population scale:
--   bank_large: 25,000 accounts
--   bank_medium: 15,000 accounts
--   bank_small: 12,000 accounts
--   bank_distressed: 8,000 accounts
-- customer_id references each bank's customers table, amounts randomly assigned

-- =========================================================================
-- 1. Standardize existing data and configure constraints for bank_large
-- =========================================================================
UPDATE bank_large.accounts 
SET account_type = 'Saving' 
WHERE account_type IN ('Savings CASA', 'Saving CASA', 'Savings', 'SAVINGS');

UPDATE bank_large.accounts 
SET account_type = 'Current' 
WHERE account_type IN ('Corporate Current', 'Current CASA', 'Current', 'CURRENT');

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_lrg_acc_type' AND connamespace = 'bank_large'::regnamespace) THEN
        ALTER TABLE bank_large.accounts 
        ADD CONSTRAINT chk_lrg_acc_type 
        CHECK (account_type IN ('Saving', 'Salary', 'Current', 'Fixed Deposit', 'Recurring Deposit', 'NRI', 'Demat',
                                'saving', 'salary', 'current', 'fixed deposit', 'recurring deposit', 'nri', 'demat'));
    END IF;
END $$;

-- Seed bank_large accounts up to 25,000 entries
INSERT INTO bank_large.accounts (account_id, customer_id, account_type, currency, available_balance, hold_amount, status, created_at)
WITH cust_arr AS (
    SELECT array_agg(customer_id) AS arr, count(*)::int AS cnt FROM bank_large.customers
),
gen AS (
    SELECT 
        s.i,
        c.arr[1 + ((abs(hashtext(s.i::text || 'lrg_ac_cust')) + floor(random() * c.cnt)::int) % c.cnt)] AS cust_id,
        (ARRAY['Saving', 'Salary', 'Current', 'Fixed Deposit', 'Recurring Deposit', 'NRI', 'Demat'])[1 + ((abs(hashtext(s.i::text || 'lrg_ac_type')) + floor(random() * 7)::int) % 7)] AS acc_type
    FROM generate_series(6, 25000) AS s(i)
    CROSS JOIN cust_arr c
)
SELECT
    'ACC-LRG-' || (5000 + g.i),
    g.cust_id,
    g.acc_type,
    'INR',
    CASE g.acc_type
        WHEN 'Current' THEN round((100000 + random() * 49000000)::numeric, 2)
        WHEN 'Fixed Deposit' THEN round((25000 + random() * 25000000)::numeric, 2)
        WHEN 'Recurring Deposit' THEN round((5000 + random() * 2000000)::numeric, 2)
        WHEN 'NRI' THEN round((200000 + random() * 30000000)::numeric, 2)
        WHEN 'Salary' THEN round((25000 + random() * 3500000)::numeric, 2)
        WHEN 'Demat' THEN round((random() * 1500000)::numeric, 2)
        ELSE round((5000 + random() * 1500000)::numeric, 2)
    END AS available_balance,
    CASE WHEN random() < 0.05 THEN round((5000 + random() * 50000)::numeric, 2) ELSE 0.00 END AS hold_amount,
    CASE WHEN random() < 0.95 THEN 'Active' WHEN random() < 0.98 THEN 'Dormant' ELSE 'Frozen' END AS status,
    TIMESTAMPTZ '2024-01-01 09:00:00+05:30' + ((abs(hashtext(g.i::text || 'lrg_ac_tm')) % 600) || ' days')::interval AS created_at
FROM gen g
ON CONFLICT (account_id) DO NOTHING;

-- =========================================================================
-- 2. Configure constraints and seed bank_medium (15,000 accounts)
-- =========================================================================
ALTER TABLE bank_medium.accounts ALTER COLUMN currency SET DEFAULT 'INR';
ALTER TABLE bank_medium.accounts ALTER COLUMN available_balance SET DEFAULT 0.00;
ALTER TABLE bank_medium.accounts ALTER COLUMN hold_amount SET DEFAULT 0.00;
ALTER TABLE bank_medium.accounts ALTER COLUMN status SET DEFAULT 'Active';
ALTER TABLE bank_medium.accounts ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'accounts_customer_id_fkey' AND connamespace = 'bank_medium'::regnamespace) THEN
        ALTER TABLE bank_medium.accounts 
        ADD CONSTRAINT accounts_customer_id_fkey 
        FOREIGN KEY (customer_id) REFERENCES bank_medium.customers(customer_id);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_med_acc_type' AND connamespace = 'bank_medium'::regnamespace) THEN
        ALTER TABLE bank_medium.accounts 
        ADD CONSTRAINT chk_med_acc_type 
        CHECK (account_type IN ('Saving', 'Salary', 'Current', 'Fixed Deposit', 'Recurring Deposit', 'NRI', 'Demat',
                                'saving', 'salary', 'current', 'fixed deposit', 'recurring deposit', 'nri', 'demat'));
    END IF;
END $$;

INSERT INTO bank_medium.accounts (account_id, customer_id, account_type, currency, available_balance, hold_amount, status, created_at)
WITH cust_arr AS (
    SELECT array_agg(customer_id) AS arr, count(*)::int AS cnt FROM bank_medium.customers
),
gen AS (
    SELECT 
        s.i,
        c.arr[1 + ((abs(hashtext(s.i::text || 'med_ac_cust')) + floor(random() * c.cnt)::int) % c.cnt)] AS cust_id,
        (ARRAY['Saving', 'Salary', 'Current', 'Fixed Deposit', 'Recurring Deposit', 'NRI', 'Demat'])[1 + ((abs(hashtext(s.i::text || 'med_ac_type')) + floor(random() * 7)::int) % 7)] AS acc_type
    FROM generate_series(1, 15000) AS s(i)
    CROSS JOIN cust_arr c
)
SELECT
    'ACC-MED-' || (5000 + g.i),
    g.cust_id,
    g.acc_type,
    'INR',
    CASE g.acc_type
        WHEN 'Current' THEN round((75000 + random() * 25000000)::numeric, 2)
        WHEN 'Fixed Deposit' THEN round((20000 + random() * 15000000)::numeric, 2)
        WHEN 'Recurring Deposit' THEN round((3000 + random() * 1500000)::numeric, 2)
        WHEN 'NRI' THEN round((150000 + random() * 20000000)::numeric, 2)
        WHEN 'Salary' THEN round((20000 + random() * 2500000)::numeric, 2)
        WHEN 'Demat' THEN round((random() * 1000000)::numeric, 2)
        ELSE round((3000 + random() * 1200000)::numeric, 2)
    END AS available_balance,
    CASE WHEN random() < 0.05 THEN round((3000 + random() * 30000)::numeric, 2) ELSE 0.00 END AS hold_amount,
    CASE WHEN random() < 0.95 THEN 'Active' WHEN random() < 0.98 THEN 'Dormant' ELSE 'Frozen' END AS status,
    TIMESTAMPTZ '2024-01-01 09:00:00+05:30' + ((abs(hashtext(g.i::text || 'med_ac_tm')) % 600) || ' days')::interval AS created_at
FROM gen g
ON CONFLICT (account_id) DO NOTHING;

-- =========================================================================
-- 3. Configure constraints and seed bank_small (12,000 accounts)
-- =========================================================================
ALTER TABLE bank_small.accounts ALTER COLUMN currency SET DEFAULT 'INR';
ALTER TABLE bank_small.accounts ALTER COLUMN available_balance SET DEFAULT 0.00;
ALTER TABLE bank_small.accounts ALTER COLUMN hold_amount SET DEFAULT 0.00;
ALTER TABLE bank_small.accounts ALTER COLUMN status SET DEFAULT 'Active';
ALTER TABLE bank_small.accounts ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'accounts_customer_id_fkey' AND connamespace = 'bank_small'::regnamespace) THEN
        ALTER TABLE bank_small.accounts 
        ADD CONSTRAINT accounts_customer_id_fkey 
        FOREIGN KEY (customer_id) REFERENCES bank_small.customers(customer_id);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_sml_acc_type' AND connamespace = 'bank_small'::regnamespace) THEN
        ALTER TABLE bank_small.accounts 
        ADD CONSTRAINT chk_sml_acc_type 
        CHECK (account_type IN ('Saving', 'Salary', 'Current', 'Fixed Deposit', 'Recurring Deposit', 'NRI', 'Demat',
                                'saving', 'salary', 'current', 'fixed deposit', 'recurring deposit', 'nri', 'demat'));
    END IF;
END $$;

INSERT INTO bank_small.accounts (account_id, customer_id, account_type, currency, available_balance, hold_amount, status, created_at)
WITH cust_arr AS (
    SELECT array_agg(customer_id) AS arr, count(*)::int AS cnt FROM bank_small.customers
),
gen AS (
    SELECT 
        s.i,
        c.arr[1 + ((abs(hashtext(s.i::text || 'sml_ac_cust')) + floor(random() * c.cnt)::int) % c.cnt)] AS cust_id,
        (ARRAY['Saving', 'Salary', 'Current', 'Fixed Deposit', 'Recurring Deposit', 'NRI', 'Demat'])[1 + ((abs(hashtext(s.i::text || 'sml_ac_type')) + floor(random() * 7)::int) % 7)] AS acc_type
    FROM generate_series(1, 12000) AS s(i)
    CROSS JOIN cust_arr c
)
SELECT
    'ACC-SML-' || (5000 + g.i),
    g.cust_id,
    g.acc_type,
    'INR',
    CASE g.acc_type
        WHEN 'Current' THEN round((50000 + random() * 15000000)::numeric, 2)
        WHEN 'Fixed Deposit' THEN round((15000 + random() * 10000000)::numeric, 2)
        WHEN 'Recurring Deposit' THEN round((2000 + random() * 1000000)::numeric, 2)
        WHEN 'NRI' THEN round((100000 + random() * 12000000)::numeric, 2)
        WHEN 'Salary' THEN round((15000 + random() * 1800000)::numeric, 2)
        WHEN 'Demat' THEN round((random() * 800000)::numeric, 2)
        ELSE round((2000 + random() * 800000)::numeric, 2)
    END AS available_balance,
    CASE WHEN random() < 0.05 THEN round((2000 + random() * 20000)::numeric, 2) ELSE 0.00 END AS hold_amount,
    CASE WHEN random() < 0.95 THEN 'Active' WHEN random() < 0.98 THEN 'Dormant' ELSE 'Frozen' END AS status,
    TIMESTAMPTZ '2024-01-01 09:00:00+05:30' + ((abs(hashtext(g.i::text || 'sml_ac_tm')) % 600) || ' days')::interval AS created_at
FROM gen g
ON CONFLICT (account_id) DO NOTHING;

-- =========================================================================
-- 4. Configure constraints and seed bank_distressed (8,000 accounts)
-- =========================================================================
ALTER TABLE bank_distressed.accounts ALTER COLUMN currency SET DEFAULT 'INR';
ALTER TABLE bank_distressed.accounts ALTER COLUMN available_balance SET DEFAULT 0.00;
ALTER TABLE bank_distressed.accounts ALTER COLUMN hold_amount SET DEFAULT 0.00;
ALTER TABLE bank_distressed.accounts ALTER COLUMN status SET DEFAULT 'Active';
ALTER TABLE bank_distressed.accounts ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'accounts_customer_id_fkey' AND connamespace = 'bank_distressed'::regnamespace) THEN
        ALTER TABLE bank_distressed.accounts 
        ADD CONSTRAINT accounts_customer_id_fkey 
        FOREIGN KEY (customer_id) REFERENCES bank_distressed.customers(customer_id);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_dst_acc_type' AND connamespace = 'bank_distressed'::regnamespace) THEN
        ALTER TABLE bank_distressed.accounts 
        ADD CONSTRAINT chk_dst_acc_type 
        CHECK (account_type IN ('Saving', 'Salary', 'Current', 'Fixed Deposit', 'Recurring Deposit', 'NRI', 'Demat',
                                'saving', 'salary', 'current', 'fixed deposit', 'recurring deposit', 'nri', 'demat'));
    END IF;
END $$;

INSERT INTO bank_distressed.accounts (account_id, customer_id, account_type, currency, available_balance, hold_amount, status, created_at)
WITH cust_arr AS (
    SELECT array_agg(customer_id) AS arr, count(*)::int AS cnt FROM bank_distressed.customers
),
gen AS (
    SELECT 
        s.i,
        c.arr[1 + ((abs(hashtext(s.i::text || 'dst_ac_cust')) + floor(random() * c.cnt)::int) % c.cnt)] AS cust_id,
        (ARRAY['Saving', 'Salary', 'Current', 'Fixed Deposit', 'Recurring Deposit', 'NRI', 'Demat'])[1 + ((abs(hashtext(s.i::text || 'dst_ac_type')) + floor(random() * 7)::int) % 7)] AS acc_type
    FROM generate_series(1, 8000) AS s(i)
    CROSS JOIN cust_arr c
)
SELECT
    'ACC-DST-' || (5000 + g.i),
    g.cust_id,
    g.acc_type,
    'INR',
    CASE g.acc_type
        WHEN 'Current' THEN round((25000 + random() * 10000000)::numeric, 2)
        WHEN 'Fixed Deposit' THEN round((10000 + random() * 6000000)::numeric, 2)
        WHEN 'Recurring Deposit' THEN round((1000 + random() * 600000)::numeric, 2)
        WHEN 'NRI' THEN round((50000 + random() * 8000000)::numeric, 2)
        WHEN 'Salary' THEN round((10000 + random() * 1200000)::numeric, 2)
        WHEN 'Demat' THEN round((random() * 500000)::numeric, 2)
        ELSE round((1000 + random() * 500000)::numeric, 2)
    END AS available_balance,
    CASE WHEN random() < 0.12 THEN round((5000 + random() * 50000)::numeric, 2) ELSE 0.00 END AS hold_amount, -- Higher lien rate in distressed
    CASE WHEN random() < 0.88 THEN 'Active' WHEN random() < 0.95 THEN 'Dormant' ELSE 'Frozen' END AS status,
    TIMESTAMPTZ '2024-01-01 09:00:00+05:30' + ((abs(hashtext(g.i::text || 'dst_ac_tm')) % 600) || ' days')::interval AS created_at
FROM gen g
ON CONFLICT (account_id) DO NOTHING;
