-- V9: Standardize loan types and seed bulk loan portfolios across all 4 bank schemas
-- Loan types: Home Loan, Mortgage Loan, Gold Loan, Vehicle Loan, Loan Against Securities, Personal Loan, Education Loan, Business Loan, Credit Card
-- Population scale:
--   bank_large: 12,000 loans (LN-LRG-00001 .. LN-LRG-12000)
--   bank_medium: 8,000 loans (LN-MED-00001 .. LN-MED-08000)
--   bank_small: 5,000 loans (LN-SML-00001 .. LN-SML-05000)
--   bank_distressed: 4,000 loans (LN-DST-00001 .. LN-DST-04000)
-- customer_id references each bank's customers table, amounts and metrics randomized

-- =========================================================================
-- 1. Configure defaults and foreign keys for all schemas
-- =========================================================================
ALTER TABLE bank_medium.loans ALTER COLUMN days_past_due SET DEFAULT 0;
ALTER TABLE bank_medium.loans ALTER COLUMN irac_classification SET DEFAULT 'Standard';
ALTER TABLE bank_medium.loans ALTER COLUMN collateral_value SET DEFAULT 0.00;

ALTER TABLE bank_small.loans ALTER COLUMN days_past_due SET DEFAULT 0;
ALTER TABLE bank_small.loans ALTER COLUMN irac_classification SET DEFAULT 'Standard';
ALTER TABLE bank_small.loans ALTER COLUMN collateral_value SET DEFAULT 0.00;

ALTER TABLE bank_distressed.loans ALTER COLUMN days_past_due SET DEFAULT 0;
ALTER TABLE bank_distressed.loans ALTER COLUMN irac_classification SET DEFAULT 'Standard';
ALTER TABLE bank_distressed.loans ALTER COLUMN collateral_value SET DEFAULT 0.00;

-- Clean existing pre-seed rows for consistent 00001-series population
TRUNCATE TABLE bank_large.loans;
TRUNCATE TABLE bank_distressed.loans;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'loans_customer_id_fkey' AND connamespace = 'bank_medium'::regnamespace) THEN
        ALTER TABLE bank_medium.loans ADD CONSTRAINT loans_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES bank_medium.customers(customer_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'loans_customer_id_fkey' AND connamespace = 'bank_small'::regnamespace) THEN
        ALTER TABLE bank_small.loans ADD CONSTRAINT loans_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES bank_small.customers(customer_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'loans_customer_id_fkey' AND connamespace = 'bank_distressed'::regnamespace) THEN
        ALTER TABLE bank_distressed.loans ADD CONSTRAINT loans_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES bank_distressed.customers(customer_id);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_lrg_loan_type' AND connamespace = 'bank_large'::regnamespace) THEN
        ALTER TABLE bank_large.loans ADD CONSTRAINT chk_lrg_loan_type 
        CHECK (loan_type IN ('Home Loan', 'Mortgage Loan', 'Gold Loan', 'Vehicle Loan', 'Loan Against Securities', 'Personal Loan', 'Education Loan', 'Business Loan', 'Credit Card',
                             'home loan', 'mortgage loan', 'gold loan', 'vehicle loan', 'loan against securities', 'personal loan', 'education loan', 'business loan', 'credit card'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_med_loan_type' AND connamespace = 'bank_medium'::regnamespace) THEN
        ALTER TABLE bank_medium.loans ADD CONSTRAINT chk_med_loan_type 
        CHECK (loan_type IN ('Home Loan', 'Mortgage Loan', 'Gold Loan', 'Vehicle Loan', 'Loan Against Securities', 'Personal Loan', 'Education Loan', 'Business Loan', 'Credit Card',
                             'home loan', 'mortgage loan', 'gold loan', 'vehicle loan', 'loan against securities', 'personal loan', 'education loan', 'business loan', 'credit card'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_sml_loan_type' AND connamespace = 'bank_small'::regnamespace) THEN
        ALTER TABLE bank_small.loans ADD CONSTRAINT chk_sml_loan_type 
        CHECK (loan_type IN ('Home Loan', 'Mortgage Loan', 'Gold Loan', 'Vehicle Loan', 'Loan Against Securities', 'Personal Loan', 'Education Loan', 'Business Loan', 'Credit Card',
                             'home loan', 'mortgage loan', 'gold loan', 'vehicle loan', 'loan against securities', 'personal loan', 'education loan', 'business loan', 'credit card'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_dst_loan_type' AND connamespace = 'bank_distressed'::regnamespace) THEN
        ALTER TABLE bank_distressed.loans ADD CONSTRAINT chk_dst_loan_type 
        CHECK (loan_type IN ('Home Loan', 'Mortgage Loan', 'Gold Loan', 'Vehicle Loan', 'Loan Against Securities', 'Personal Loan', 'Education Loan', 'Business Loan', 'Credit Card',
                             'home loan', 'mortgage loan', 'gold loan', 'vehicle loan', 'loan against securities', 'personal loan', 'education loan', 'business loan', 'credit card'));
    END IF;
END $$;

-- =========================================================================
-- 2. bank_large (12,000 loans, healthy low-NPA profile)
-- =========================================================================
INSERT INTO bank_large.loans (
    loan_id, customer_id, loan_type, sanctioned_amount, principal_outstanding,
    interest_rate_pct, days_past_due, irac_classification, collateral_value,
    next_emi_due_date, monthly_emi
)
WITH cust_arr AS (
    SELECT array_agg(customer_id) AS arr, count(*)::int AS cnt FROM bank_large.customers
),
gen AS (
    SELECT 
        s.i,
        c.arr[1 + ((abs(hashtext(s.i::text || 'lrg_ln_cust')) + floor(random() * c.cnt)::int) % c.cnt)] AS cust_id,
        (ARRAY['Home Loan', 'Mortgage Loan', 'Gold Loan', 'Vehicle Loan', 'Loan Against Securities', 'Personal Loan', 'Education Loan', 'Business Loan', 'Credit Card'])[1 + ((abs(hashtext(s.i::text || 'lrg_ln_type')) + floor(random() * 9)::int) % 9)] AS l_type
    FROM generate_series(1, 12000) AS s(i)
    CROSS JOIN cust_arr c
),
calc AS (
    SELECT 
        g.i,
        g.cust_id,
        g.l_type,
        CASE g.l_type
            WHEN 'Home Loan' THEN round((2000000 + random() * 18000000)::numeric, 2)
            WHEN 'Mortgage Loan' THEN round((2500000 + random() * 27500000)::numeric, 2)
            WHEN 'Gold Loan' THEN round((50000 + random() * 1950000)::numeric, 2)
            WHEN 'Vehicle Loan' THEN round((300000 + random() * 2700000)::numeric, 2)
            WHEN 'Loan Against Securities' THEN round((200000 + random() * 9800000)::numeric, 2)
            WHEN 'Personal Loan' THEN round((100000 + random() * 1900000)::numeric, 2)
            WHEN 'Education Loan' THEN round((300000 + random() * 4700000)::numeric, 2)
            WHEN 'Business Loan' THEN round((1000000 + random() * 49000000)::numeric, 2)
            ELSE round((50000 + random() * 950000)::numeric, 2)
        END AS sanc_amt,
        CASE g.l_type
            WHEN 'Home Loan' THEN round((8.25 + random() * 1.00)::numeric, 2)
            WHEN 'Mortgage Loan' THEN round((9.00 + random() * 2.00)::numeric, 2)
            WHEN 'Gold Loan' THEN round((9.50 + random() * 3.00)::numeric, 2)
            WHEN 'Vehicle Loan' THEN round((8.75 + random() * 2.50)::numeric, 2)
            WHEN 'Loan Against Securities' THEN round((9.25 + random() * 2.00)::numeric, 2)
            WHEN 'Personal Loan' THEN round((11.50 + random() * 4.50)::numeric, 2)
            WHEN 'Education Loan' THEN round((8.50 + random() * 2.50)::numeric, 2)
            WHEN 'Business Loan' THEN round((9.75 + random() * 3.75)::numeric, 2)
            ELSE round((36.00 + random() * 6.00)::numeric, 2)
        END AS int_rate,
        CASE 
            WHEN random() < 0.95 THEN 0
            WHEN random() < 0.98 THEN 1 + (abs(hashtext(g.i::text || 'lrg_dpd1')) % 30)
            WHEN random() < 0.995 THEN 31 + (abs(hashtext(g.i::text || 'lrg_dpd2')) % 59)
            ELSE 91 + (abs(hashtext(g.i::text || 'lrg_dpd3')) % 90)
        END AS dpd
    FROM gen g
)
SELECT
    'LN-LRG-' || lpad(c.i::text, 5, '0') AS loan_id,
    c.cust_id,
    c.l_type AS loan_type,
    c.sanc_amt AS sanctioned_amount,
    round((c.sanc_amt * (0.50 + random() * 0.45))::numeric, 2) AS principal_outstanding,
    c.int_rate AS interest_rate_pct,
    c.dpd AS days_past_due,
    CASE 
        WHEN c.dpd > 90 THEN 'Sub-Standard NPA'
        WHEN c.dpd > 60 THEN 'SMA-2'
        WHEN c.dpd > 30 THEN 'SMA-1'
        WHEN c.dpd > 0 THEN 'SMA-0'
        ELSE 'Standard'
    END AS irac_classification,
    CASE 
        WHEN c.l_type IN ('Personal Loan', 'Credit Card') THEN 0.00
        WHEN c.l_type = 'Education Loan' AND c.sanc_amt < 750000 THEN 0.00
        ELSE round((c.sanc_amt * (1.20 + random() * 0.40))::numeric, 2)
    END AS collateral_value,
    CURRENT_DATE + ((abs(hashtext(c.i::text || 'lrg_due')) % 30) + 1) * interval '1 day' AS next_emi_due_date,
    round((c.sanc_amt * CASE WHEN c.l_type = 'Credit Card' THEN 0.05 ELSE 0.015 END)::numeric, 2) AS monthly_emi
FROM calc c;

-- =========================================================================
-- 3. bank_medium (8,000 loans, moderate-NPA profile)
-- =========================================================================
INSERT INTO bank_medium.loans (
    loan_id, customer_id, loan_type, sanctioned_amount, principal_outstanding,
    interest_rate_pct, days_past_due, irac_classification, collateral_value,
    next_emi_due_date, monthly_emi
)
WITH cust_arr AS (
    SELECT array_agg(customer_id) AS arr, count(*)::int AS cnt FROM bank_medium.customers
),
gen AS (
    SELECT 
        s.i,
        c.arr[1 + ((abs(hashtext(s.i::text || 'med_ln_cust')) + floor(random() * c.cnt)::int) % c.cnt)] AS cust_id,
        (ARRAY['Home Loan', 'Mortgage Loan', 'Gold Loan', 'Vehicle Loan', 'Loan Against Securities', 'Personal Loan', 'Education Loan', 'Business Loan', 'Credit Card'])[1 + ((abs(hashtext(s.i::text || 'med_ln_type')) + floor(random() * 9)::int) % 9)] AS l_type
    FROM generate_series(1, 8000) AS s(i)
    CROSS JOIN cust_arr c
),
calc AS (
    SELECT 
        g.i,
        g.cust_id,
        g.l_type,
        CASE g.l_type
            WHEN 'Home Loan' THEN round((1500000 + random() * 12000000)::numeric, 2)
            WHEN 'Mortgage Loan' THEN round((2000000 + random() * 18000000)::numeric, 2)
            WHEN 'Gold Loan' THEN round((40000 + random() * 1500000)::numeric, 2)
            WHEN 'Vehicle Loan' THEN round((250000 + random() * 2000000)::numeric, 2)
            WHEN 'Loan Against Securities' THEN round((150000 + random() * 6000000)::numeric, 2)
            WHEN 'Personal Loan' THEN round((80000 + random() * 1400000)::numeric, 2)
            WHEN 'Education Loan' THEN round((250000 + random() * 3500000)::numeric, 2)
            WHEN 'Business Loan' THEN round((800000 + random() * 30000000)::numeric, 2)
            ELSE round((40000 + random() * 700000)::numeric, 2)
        END AS sanc_amt,
        CASE g.l_type
            WHEN 'Home Loan' THEN round((8.50 + random() * 1.25)::numeric, 2)
            WHEN 'Mortgage Loan' THEN round((9.25 + random() * 2.25)::numeric, 2)
            WHEN 'Gold Loan' THEN round((9.75 + random() * 3.25)::numeric, 2)
            WHEN 'Vehicle Loan' THEN round((9.00 + random() * 2.50)::numeric, 2)
            WHEN 'Loan Against Securities' THEN round((9.50 + random() * 2.25)::numeric, 2)
            WHEN 'Personal Loan' THEN round((12.00 + random() * 4.50)::numeric, 2)
            WHEN 'Education Loan' THEN round((8.75 + random() * 2.50)::numeric, 2)
            WHEN 'Business Loan' THEN round((10.25 + random() * 4.00)::numeric, 2)
            ELSE round((36.00 + random() * 6.00)::numeric, 2)
        END AS int_rate,
        CASE 
            WHEN random() < 0.92 THEN 0
            WHEN random() < 0.96 THEN 1 + (abs(hashtext(g.i::text || 'med_dpd1')) % 30)
            WHEN random() < 0.98 THEN 31 + (abs(hashtext(g.i::text || 'med_dpd2')) % 59)
            ELSE 91 + (abs(hashtext(g.i::text || 'med_dpd3')) % 90)
        END AS dpd
    FROM gen g
)
SELECT
    'LN-MED-' || lpad(c.i::text, 5, '0') AS loan_id,
    c.cust_id,
    c.l_type AS loan_type,
    c.sanc_amt AS sanctioned_amount,
    round((c.sanc_amt * (0.50 + random() * 0.45))::numeric, 2) AS principal_outstanding,
    c.int_rate AS interest_rate_pct,
    c.dpd AS days_past_due,
    CASE 
        WHEN c.dpd > 90 THEN 'Sub-Standard NPA'
        WHEN c.dpd > 60 THEN 'SMA-2'
        WHEN c.dpd > 30 THEN 'SMA-1'
        WHEN c.dpd > 0 THEN 'SMA-0'
        ELSE 'Standard'
    END AS irac_classification,
    CASE 
        WHEN c.l_type IN ('Personal Loan', 'Credit Card') THEN 0.00
        WHEN c.l_type = 'Education Loan' AND c.sanc_amt < 750000 THEN 0.00
        ELSE round((c.sanc_amt * (1.20 + random() * 0.40))::numeric, 2)
    END AS collateral_value,
    CURRENT_DATE + ((abs(hashtext(c.i::text || 'med_due')) % 30) + 1) * interval '1 day' AS next_emi_due_date,
    round((c.sanc_amt * CASE WHEN c.l_type = 'Credit Card' THEN 0.05 ELSE 0.015 END)::numeric, 2) AS monthly_emi
FROM calc c;

-- =========================================================================
-- 4. bank_small (5,000 loans, urban co-operative profile)
-- =========================================================================
INSERT INTO bank_small.loans (
    loan_id, customer_id, loan_type, sanctioned_amount, principal_outstanding,
    interest_rate_pct, days_past_due, irac_classification, collateral_value,
    next_emi_due_date, monthly_emi
)
WITH cust_arr AS (
    SELECT array_agg(customer_id) AS arr, count(*)::int AS cnt FROM bank_small.customers
),
gen AS (
    SELECT 
        s.i,
        c.arr[1 + ((abs(hashtext(s.i::text || 'sml_ln_cust')) + floor(random() * c.cnt)::int) % c.cnt)] AS cust_id,
        (ARRAY['Home Loan', 'Mortgage Loan', 'Gold Loan', 'Vehicle Loan', 'Loan Against Securities', 'Personal Loan', 'Education Loan', 'Business Loan', 'Credit Card'])[1 + ((abs(hashtext(s.i::text || 'sml_ln_type')) + floor(random() * 9)::int) % 9)] AS l_type
    FROM generate_series(1, 5000) AS s(i)
    CROSS JOIN cust_arr c
),
calc AS (
    SELECT 
        g.i,
        g.cust_id,
        g.l_type,
        CASE g.l_type
            WHEN 'Home Loan' THEN round((1000000 + random() * 8000000)::numeric, 2)
            WHEN 'Mortgage Loan' THEN round((1500000 + random() * 12000000)::numeric, 2)
            WHEN 'Gold Loan' THEN round((30000 + random() * 1200000)::numeric, 2)
            WHEN 'Vehicle Loan' THEN round((150000 + random() * 1500000)::numeric, 2)
            WHEN 'Loan Against Securities' THEN round((100000 + random() * 4000000)::numeric, 2)
            WHEN 'Personal Loan' THEN round((50000 + random() * 900000)::numeric, 2)
            WHEN 'Education Loan' THEN round((200000 + random() * 2500000)::numeric, 2)
            WHEN 'Business Loan' THEN round((500000 + random() * 15000000)::numeric, 2)
            ELSE round((30000 + random() * 500000)::numeric, 2)
        END AS sanc_amt,
        CASE g.l_type
            WHEN 'Home Loan' THEN round((8.75 + random() * 1.50)::numeric, 2)
            WHEN 'Mortgage Loan' THEN round((9.50 + random() * 2.25)::numeric, 2)
            WHEN 'Gold Loan' THEN round((10.00 + random() * 3.50)::numeric, 2)
            WHEN 'Vehicle Loan' THEN round((9.25 + random() * 2.50)::numeric, 2)
            WHEN 'Loan Against Securities' THEN round((9.75 + random() * 2.25)::numeric, 2)
            WHEN 'Personal Loan' THEN round((12.50 + random() * 4.50)::numeric, 2)
            WHEN 'Education Loan' THEN round((9.00 + random() * 2.50)::numeric, 2)
            WHEN 'Business Loan' THEN round((10.50 + random() * 4.00)::numeric, 2)
            ELSE round((36.00 + random() * 6.00)::numeric, 2)
        END AS int_rate,
        CASE 
            WHEN random() < 0.93 THEN 0
            WHEN random() < 0.96 THEN 1 + (abs(hashtext(g.i::text || 'sml_dpd1')) % 30)
            WHEN random() < 0.98 THEN 31 + (abs(hashtext(g.i::text || 'sml_dpd2')) % 59)
            ELSE 91 + (abs(hashtext(g.i::text || 'sml_dpd3')) % 90)
        END AS dpd
    FROM gen g
)
SELECT
    'LN-SML-' || lpad(c.i::text, 5, '0') AS loan_id,
    c.cust_id,
    c.l_type AS loan_type,
    c.sanc_amt AS sanctioned_amount,
    round((c.sanc_amt * (0.50 + random() * 0.45))::numeric, 2) AS principal_outstanding,
    c.int_rate AS interest_rate_pct,
    c.dpd AS days_past_due,
    CASE 
        WHEN c.dpd > 90 THEN 'Sub-Standard NPA'
        WHEN c.dpd > 60 THEN 'SMA-2'
        WHEN c.dpd > 30 THEN 'SMA-1'
        WHEN c.dpd > 0 THEN 'SMA-0'
        ELSE 'Standard'
    END AS irac_classification,
    CASE 
        WHEN c.l_type IN ('Personal Loan', 'Credit Card') THEN 0.00
        WHEN c.l_type = 'Education Loan' AND c.sanc_amt < 750000 THEN 0.00
        ELSE round((c.sanc_amt * (1.20 + random() * 0.40))::numeric, 2)
    END AS collateral_value,
    CURRENT_DATE + ((abs(hashtext(c.i::text || 'sml_due')) % 30) + 1) * interval '1 day' AS next_emi_due_date,
    round((c.sanc_amt * CASE WHEN c.l_type = 'Credit Card' THEN 0.05 ELSE 0.015 END)::numeric, 2) AS monthly_emi
FROM calc c;

-- =========================================================================
-- 5. bank_distressed (4,000 loans, high-NPA stressed profile > 14% NPA)
-- =========================================================================
INSERT INTO bank_distressed.loans (
    loan_id, customer_id, loan_type, sanctioned_amount, principal_outstanding,
    interest_rate_pct, days_past_due, irac_classification, collateral_value,
    next_emi_due_date, monthly_emi
)
WITH cust_arr AS (
    SELECT array_agg(customer_id) AS arr, count(*)::int AS cnt FROM bank_distressed.customers
),
gen AS (
    SELECT 
        s.i,
        c.arr[1 + ((abs(hashtext(s.i::text || 'dst_ln_cust')) + floor(random() * c.cnt)::int) % c.cnt)] AS cust_id,
        (ARRAY['Home Loan', 'Mortgage Loan', 'Gold Loan', 'Vehicle Loan', 'Loan Against Securities', 'Personal Loan', 'Education Loan', 'Business Loan', 'Credit Card'])[1 + ((abs(hashtext(s.i::text || 'dst_ln_type')) + floor(random() * 9)::int) % 9)] AS l_type
    FROM generate_series(1, 4000) AS s(i)
    CROSS JOIN cust_arr c
),
calc AS (
    SELECT 
        g.i,
        g.cust_id,
        g.l_type,
        CASE g.l_type
            WHEN 'Home Loan' THEN round((1000000 + random() * 7000000)::numeric, 2)
            WHEN 'Mortgage Loan' THEN round((1500000 + random() * 10000000)::numeric, 2)
            WHEN 'Gold Loan' THEN round((25000 + random() * 1000000)::numeric, 2)
            WHEN 'Vehicle Loan' THEN round((150000 + random() * 1200000)::numeric, 2)
            WHEN 'Loan Against Securities' THEN round((100000 + random() * 3000000)::numeric, 2)
            WHEN 'Personal Loan' THEN round((40000 + random() * 800000)::numeric, 2)
            WHEN 'Education Loan' THEN round((150000 + random() * 2000000)::numeric, 2)
            WHEN 'Business Loan' THEN round((500000 + random() * 12000000)::numeric, 2)
            ELSE round((25000 + random() * 400000)::numeric, 2)
        END AS sanc_amt,
        CASE g.l_type
            WHEN 'Home Loan' THEN round((9.50 + random() * 2.00)::numeric, 2)
            WHEN 'Mortgage Loan' THEN round((10.50 + random() * 2.50)::numeric, 2)
            WHEN 'Gold Loan' THEN round((11.00 + random() * 3.50)::numeric, 2)
            WHEN 'Vehicle Loan' THEN round((10.25 + random() * 3.00)::numeric, 2)
            WHEN 'Loan Against Securities' THEN round((10.50 + random() * 2.50)::numeric, 2)
            WHEN 'Personal Loan' THEN round((13.50 + random() * 5.00)::numeric, 2)
            WHEN 'Education Loan' THEN round((9.75 + random() * 3.00)::numeric, 2)
            WHEN 'Business Loan' THEN round((11.50 + random() * 4.50)::numeric, 2)
            ELSE round((38.00 + random() * 6.00)::numeric, 2)
        END AS int_rate,
        CASE 
            WHEN random() < 0.65 THEN 0
            WHEN random() < 0.75 THEN 1 + (abs(hashtext(g.i::text || 'dst_dpd1')) % 30)
            WHEN random() < 0.81 THEN 31 + (abs(hashtext(g.i::text || 'dst_dpd2')) % 29)
            WHEN random() < 0.86 THEN 61 + (abs(hashtext(g.i::text || 'dst_dpd3')) % 29)
            WHEN random() < 0.96 THEN 91 + (abs(hashtext(g.i::text || 'dst_dpd4')) % 89)
            ELSE 181 + (abs(hashtext(g.i::text || 'dst_dpd5')) % 184)
        END AS dpd
    FROM gen g
)
SELECT
    'LN-DST-' || lpad(c.i::text, 5, '0') AS loan_id,
    c.cust_id,
    c.l_type AS loan_type,
    c.sanc_amt AS sanctioned_amount,
    round((c.sanc_amt * (0.60 + random() * 0.38))::numeric, 2) AS principal_outstanding,
    c.int_rate AS interest_rate_pct,
    c.dpd AS days_past_due,
    CASE 
        WHEN c.dpd > 180 THEN 'Doubtful NPA'
        WHEN c.dpd > 90 THEN 'Sub-Standard NPA'
        WHEN c.dpd > 60 THEN 'SMA-2'
        WHEN c.dpd > 30 THEN 'SMA-1'
        WHEN c.dpd > 0 THEN 'SMA-0'
        ELSE 'Standard'
    END AS irac_classification,
    CASE 
        WHEN c.l_type IN ('Personal Loan', 'Credit Card') THEN 0.00
        WHEN c.l_type = 'Education Loan' AND c.sanc_amt < 750000 THEN 0.00
        ELSE round((c.sanc_amt * (0.90 + random() * 0.40))::numeric, 2) -- Impaired collateral valuation
    END AS collateral_value,
    CURRENT_DATE + ((abs(hashtext(c.i::text || 'dst_due')) % 30) + 1) * interval '1 day' AS next_emi_due_date,
    round((c.sanc_amt * CASE WHEN c.l_type = 'Credit Card' THEN 0.05 ELSE 0.015 END)::numeric, 2) AS monthly_emi
FROM calc c;
