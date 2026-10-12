-- V11: Update treasury holding categories across all 4 bank schemas
-- New standardized categories:
--   'Government Bond', 'Treasury Bill', 'Corporate Bond', 'Commercial Paper',
--   'Government Securities', 'Municipal Bond', 'Mortgage-backed Securities',
--   'ETF', 'Gold commodities'

-- 1. Drop existing category check constraints
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_lrg_treasury_cat' AND connamespace = 'bank_large'::regnamespace) THEN
        ALTER TABLE bank_large.treasury_holdings DROP CONSTRAINT chk_lrg_treasury_cat;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_med_treasury_cat' AND connamespace = 'bank_medium'::regnamespace) THEN
        ALTER TABLE bank_medium.treasury_holdings DROP CONSTRAINT chk_med_treasury_cat;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_sml_treasury_cat' AND connamespace = 'bank_small'::regnamespace) THEN
        ALTER TABLE bank_small.treasury_holdings DROP CONSTRAINT chk_sml_treasury_cat;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_dst_treasury_cat' AND connamespace = 'bank_distressed'::regnamespace) THEN
        ALTER TABLE bank_distressed.treasury_holdings DROP CONSTRAINT chk_dst_treasury_cat;
    END IF;
END $$;

-- 2. Update all categories across all 4 schemas
UPDATE bank_large.treasury_holdings
SET category = CASE 
    WHEN category ILIKE '%government%bond%' THEN 'Government Bond'
    WHEN category ILIKE '%treasury%bill%' THEN 'Treasury Bill'
    WHEN category ILIKE '%corporate%bond%' THEN 'Corporate Bond'
    WHEN category ILIKE '%commercial%paper%' THEN 'Commercial Paper'
    WHEN category ILIKE '%government%securit%' THEN 'Government Securities'
    WHEN category ILIKE '%municipal%bond%' THEN 'Municipal Bond'
    WHEN category ILIKE '%mortgage%' THEN 'Mortgage-backed Securities'
    WHEN category ILIKE '%etf%' OR category ILIKE '%bond%etf%' THEN 'ETF'
    WHEN category ILIKE '%gold%' THEN 'Gold commodities'
    ELSE (ARRAY[
        'Government Bond', 'Treasury Bill', 'Corporate Bond', 'Commercial Paper',
        'Government Securities', 'Municipal Bond', 'Mortgage-backed Securities',
        'ETF', 'Gold commodities'
    ])[1 + (abs(hashtext(holding_id)) % 9)]
END;

UPDATE bank_medium.treasury_holdings
SET category = CASE 
    WHEN category ILIKE '%government%bond%' THEN 'Government Bond'
    WHEN category ILIKE '%treasury%bill%' THEN 'Treasury Bill'
    WHEN category ILIKE '%corporate%bond%' THEN 'Corporate Bond'
    WHEN category ILIKE '%commercial%paper%' THEN 'Commercial Paper'
    WHEN category ILIKE '%government%securit%' THEN 'Government Securities'
    WHEN category ILIKE '%municipal%bond%' THEN 'Municipal Bond'
    WHEN category ILIKE '%mortgage%' THEN 'Mortgage-backed Securities'
    WHEN category ILIKE '%etf%' OR category ILIKE '%bond%etf%' THEN 'ETF'
    WHEN category ILIKE '%gold%' THEN 'Gold commodities'
    ELSE (ARRAY[
        'Government Bond', 'Treasury Bill', 'Corporate Bond', 'Commercial Paper',
        'Government Securities', 'Municipal Bond', 'Mortgage-backed Securities',
        'ETF', 'Gold commodities'
    ])[1 + (abs(hashtext(holding_id)) % 9)]
END;

UPDATE bank_small.treasury_holdings
SET category = CASE 
    WHEN category ILIKE '%government%bond%' THEN 'Government Bond'
    WHEN category ILIKE '%treasury%bill%' THEN 'Treasury Bill'
    WHEN category ILIKE '%corporate%bond%' THEN 'Corporate Bond'
    WHEN category ILIKE '%commercial%paper%' THEN 'Commercial Paper'
    WHEN category ILIKE '%government%securit%' THEN 'Government Securities'
    WHEN category ILIKE '%municipal%bond%' THEN 'Municipal Bond'
    WHEN category ILIKE '%mortgage%' THEN 'Mortgage-backed Securities'
    WHEN category ILIKE '%etf%' OR category ILIKE '%bond%etf%' THEN 'ETF'
    WHEN category ILIKE '%gold%' THEN 'Gold commodities'
    ELSE (ARRAY[
        'Government Bond', 'Treasury Bill', 'Corporate Bond', 'Commercial Paper',
        'Government Securities', 'Municipal Bond', 'Mortgage-backed Securities',
        'ETF', 'Gold commodities'
    ])[1 + (abs(hashtext(holding_id)) % 9)]
END;

UPDATE bank_distressed.treasury_holdings
SET category = CASE 
    WHEN category ILIKE '%government%bond%' THEN 'Government Bond'
    WHEN category ILIKE '%treasury%bill%' THEN 'Treasury Bill'
    WHEN category ILIKE '%corporate%bond%' THEN 'Corporate Bond'
    WHEN category ILIKE '%commercial%paper%' THEN 'Commercial Paper'
    WHEN category ILIKE '%government%securit%' THEN 'Government Securities'
    WHEN category ILIKE '%municipal%bond%' THEN 'Municipal Bond'
    WHEN category ILIKE '%mortgage%' THEN 'Mortgage-backed Securities'
    WHEN category ILIKE '%etf%' OR category ILIKE '%bond%etf%' THEN 'ETF'
    WHEN category ILIKE '%gold%' THEN 'Gold commodities'
    ELSE (ARRAY[
        'Government Bond', 'Treasury Bill', 'Corporate Bond', 'Commercial Paper',
        'Government Securities', 'Municipal Bond', 'Mortgage-backed Securities',
        'ETF', 'Gold commodities'
    ])[1 + (abs(hashtext(holding_id)) % 9)]
END;

-- 3. Add new check constraints for updated categories
ALTER TABLE bank_large.treasury_holdings ADD CONSTRAINT chk_lrg_treasury_cat 
CHECK (category IN (
    'Government Bond', 'Treasury Bill', 'Corporate Bond', 'Commercial Paper',
    'Government Securities', 'Municipal Bond', 'Mortgage-backed Securities',
    'ETF', 'Gold commodities'
));

ALTER TABLE bank_medium.treasury_holdings ADD CONSTRAINT chk_med_treasury_cat 
CHECK (category IN (
    'Government Bond', 'Treasury Bill', 'Corporate Bond', 'Commercial Paper',
    'Government Securities', 'Municipal Bond', 'Mortgage-backed Securities',
    'ETF', 'Gold commodities'
));

ALTER TABLE bank_small.treasury_holdings ADD CONSTRAINT chk_sml_treasury_cat 
CHECK (category IN (
    'Government Bond', 'Treasury Bill', 'Corporate Bond', 'Commercial Paper',
    'Government Securities', 'Municipal Bond', 'Mortgage-backed Securities',
    'ETF', 'Gold commodities'
));

ALTER TABLE bank_distressed.treasury_holdings ADD CONSTRAINT chk_dst_treasury_cat 
CHECK (category IN (
    'Government Bond', 'Treasury Bill', 'Corporate Bond', 'Commercial Paper',
    'Government Securities', 'Municipal Bond', 'Mortgage-backed Securities',
    'ETF', 'Gold commodities'
));
