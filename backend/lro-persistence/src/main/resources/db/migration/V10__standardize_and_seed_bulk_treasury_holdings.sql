-- V10: Standardize treasury holding categories and seed bulk portfolios across all 4 bank schemas
-- Categories (9 types):
--   'Government bond', 'Treasury bill', 'Corporate bond', 'commercial paper',
--   'government securities', 'municipal bond', 'mortgage backed securities',
--   'bond etf', 'gold commodities'
-- Population scale:
--   bank_large: 5,000 treasury holdings (TRS-LRG-00001 .. TRS-LRG-05000)
--   bank_medium: 3,000 treasury holdings (TRS-MED-00001 .. TRS-MED-03000)
--   bank_small: 2,500 treasury holdings (TRS-SML-00001 .. TRS-SML-02500)
--   bank_distressed: 1,500 treasury holdings (TRS-DST-00001 .. TRS-DST-01500)
-- Amount and quantity randomly distributed across each asset class

-- =========================================================================
-- 1. Configure defaults and constraints for all schemas
-- =========================================================================
ALTER TABLE bank_medium.treasury_holdings ALTER COLUMN encumbrance_status SET DEFAULT 'Unencumbered';
ALTER TABLE bank_medium.treasury_holdings ALTER COLUMN coupon_or_yield_pct SET DEFAULT 0.00;
ALTER TABLE bank_medium.treasury_holdings ALTER COLUMN last_valuation_time SET DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE bank_small.treasury_holdings ALTER COLUMN encumbrance_status SET DEFAULT 'Unencumbered';
ALTER TABLE bank_small.treasury_holdings ALTER COLUMN coupon_or_yield_pct SET DEFAULT 0.00;
ALTER TABLE bank_small.treasury_holdings ALTER COLUMN last_valuation_time SET DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE bank_distressed.treasury_holdings ALTER COLUMN encumbrance_status SET DEFAULT 'Unencumbered';
ALTER TABLE bank_distressed.treasury_holdings ALTER COLUMN coupon_or_yield_pct SET DEFAULT 0.00;
ALTER TABLE bank_distressed.treasury_holdings ALTER COLUMN last_valuation_time SET DEFAULT CURRENT_TIMESTAMP;

-- Clean existing pre-seed rows for consistent 00001-series population
TRUNCATE TABLE bank_large.treasury_holdings;
TRUNCATE TABLE bank_medium.treasury_holdings;
TRUNCATE TABLE bank_small.treasury_holdings;
TRUNCATE TABLE bank_distressed.treasury_holdings;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_lrg_treasury_cat' AND connamespace = 'bank_large'::regnamespace) THEN
        ALTER TABLE bank_large.treasury_holdings ADD CONSTRAINT chk_lrg_treasury_cat 
        CHECK (category IN (
            'Government bond', 'Treasury bill', 'Corporate bond', 'commercial paper', 
            'government securities', 'municipal bond', 'mortgage backed securities', 
            'bond etf', 'gold commodities',
            'Government Bond', 'Treasury Bill', 'Corporate Bond', 'Commercial Paper', 
            'Government Securities', 'Municipal Bond', 'Mortgage Backed Securities', 
            'Bond ETF', 'Gold Commodities'
        ));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_med_treasury_cat' AND connamespace = 'bank_medium'::regnamespace) THEN
        ALTER TABLE bank_medium.treasury_holdings ADD CONSTRAINT chk_med_treasury_cat 
        CHECK (category IN (
            'Government bond', 'Treasury bill', 'Corporate bond', 'commercial paper', 
            'government securities', 'municipal bond', 'mortgage backed securities', 
            'bond etf', 'gold commodities',
            'Government Bond', 'Treasury Bill', 'Corporate Bond', 'Commercial Paper', 
            'Government Securities', 'Municipal Bond', 'Mortgage Backed Securities', 
            'Bond ETF', 'Gold Commodities'
        ));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_sml_treasury_cat' AND connamespace = 'bank_small'::regnamespace) THEN
        ALTER TABLE bank_small.treasury_holdings ADD CONSTRAINT chk_sml_treasury_cat 
        CHECK (category IN (
            'Government bond', 'Treasury bill', 'Corporate bond', 'commercial paper', 
            'government securities', 'municipal bond', 'mortgage backed securities', 
            'bond etf', 'gold commodities',
            'Government Bond', 'Treasury Bill', 'Corporate Bond', 'Commercial Paper', 
            'Government Securities', 'Municipal Bond', 'Mortgage Backed Securities', 
            'Bond ETF', 'Gold Commodities'
        ));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_dst_treasury_cat' AND connamespace = 'bank_distressed'::regnamespace) THEN
        ALTER TABLE bank_distressed.treasury_holdings ADD CONSTRAINT chk_dst_treasury_cat 
        CHECK (category IN (
            'Government bond', 'Treasury bill', 'Corporate bond', 'commercial paper', 
            'government securities', 'municipal bond', 'mortgage backed securities', 
            'bond etf', 'gold commodities',
            'Government Bond', 'Treasury Bill', 'Corporate Bond', 'Commercial Paper', 
            'Government Securities', 'Municipal Bond', 'Mortgage Backed Securities', 
            'Bond ETF', 'Gold Commodities'
        ));
    END IF;
END $$;

-- =========================================================================
-- 2. bank_large (5,000 treasury holdings)
-- =========================================================================
INSERT INTO bank_large.treasury_holdings (
    holding_id, isin, instrument_name, category, quantity, face_value_unit,
    book_value, market_value, encumbrance_status, maturity_date, coupon_or_yield_pct, last_valuation_time
)
WITH gen AS (
    SELECT 
        s.i,
        (ARRAY[
            'Government bond', 
            'Treasury bill', 
            'Corporate bond', 
            'commercial paper', 
            'government securities', 
            'municipal bond', 
            'mortgage backed securities', 
            'bond etf', 
            'gold commodities'
        ])[1 + ((abs(hashtext(s.i::text || 'lrg_cat')) + floor(random() * 9)::int) % 9)] AS cat
    FROM generate_series(1, 5000) AS s(i)
),
calc AS (
    SELECT
        g.i,
        g.cat,
        CASE g.cat
            WHEN 'Government bond' THEN (ARRAY['7.18% GS 2033 Benchmark', '7.26% GS 2032', '7.06% GS 2028', '7.38% GS 2027', '6.54% GS 2032', '7.10% GS 2034', '7.40% GS 2035', '6.99% GS 2051', '7.54% GS 2036', '7.17% GS 2028'])[1 + (g.i % 10)]
            WHEN 'Treasury bill' THEN CASE (g.i % 3) WHEN 0 THEN '91-Day T-Bill Series-' || (g.i % 40 + 1) WHEN 1 THEN '182-Day T-Bill Series-' || (g.i % 40 + 1) ELSE '364-Day T-Bill Series-' || (g.i % 40 + 1) END
            WHEN 'Corporate bond' THEN (ARRAY['8.15% HDFC Bank AAA Bond 2029', '7.95% ICICI Bank Tier-2 2031', '8.40% REC Limited Bond 2028', '8.20% PFC Infrastructure Bond 2030', '7.85% Bajaj Finance Senior NCD 2027', '8.50% L&T Finance Debenture 2029', '8.65% Tata Capital Bond 2028', '8.10% NTPC PSU Bond 2032'])[1 + (g.i % 8)]
            WHEN 'commercial paper' THEN (ARRAY['Reliance Retail CP 90D', 'Tata Motors Finance CP 180D', 'Adani Ports CP 90D', 'UltraTech Cement CP 60D', 'Mahindra & Mahindra Fin CP 120D', 'L&T Finance Holdings CP 90D'])[1 + (g.i % 6)]
            WHEN 'government securities' THEN (ARRAY['7.25% Sovereign Green Bond 2028', '7.30% SDL (Maharashtra) 2033', '7.42% SDL (Gujarat) 2031', '7.35% SDL (Tamil Nadu) 2032', '7.39% SDL (Karnataka) 2034', '7.45% SDL (Uttar Pradesh) 2033'])[1 + (g.i % 6)]
            WHEN 'municipal bond' THEN (ARRAY['8.50% Ahmedabad Municipal Green Bond 2029', '8.40% Pune Municipal Corp Bond 2028', '8.25% Vadodara Municipal Corp 2027', '8.60% Indore Municipal Green Bond 2030', '8.30% Surat Municipal Bond 2029'])[1 + (g.i % 5)]
            WHEN 'mortgage backed securities' THEN (ARRAY['HDFC Capital RMBS Tranche-A1', 'LIC Housing RMBS Trust Series-1', 'PNB Housing Prime MBS Class-A', 'Can Fin Homes Securitized Note 2028', 'Bajaj Housing Finance RMBS 2029'])[1 + (g.i % 5)]
            WHEN 'bond etf' THEN (ARRAY['Bharat Bond ETF - April 2030', 'Bharat Bond ETF - April 2032', 'Nippon India ETF Nifty CPSE Bond', 'Edelweiss Nifty PSU Bond Plus SDL ETF', 'Mirae Asset Nifty SDL June 2027 ETF'])[1 + (g.i % 5)]
            ELSE (ARRAY['Sovereign Gold Bond 2028 Series-VI', 'Sovereign Gold Bond 2029 Series-II', 'Nippon India Gold ETF Units', 'HDFC Gold ETF Holding', 'SBI Gold Sovereign Lot'])[1 + (g.i % 5)]
        END AS inst_name,
        CASE g.cat
            WHEN 'Government bond' THEN 'IN0020' || lpad((20 + (g.i % 10))::text, 2, '0') || lpad((g.i % 9000 + 1000)::text, 4, '0')
            WHEN 'Treasury bill' THEN 'IN002024' || (ARRAY['X', 'Y', 'Z'])[1 + (g.i % 3)] || lpad((g.i % 900 + 100)::text, 3, '0')
            WHEN 'Corporate bond' THEN 'INE' || lpad((g.i % 900 + 100)::text, 3, '0') || 'A08' || lpad((g.i % 90 + 10)::text, 2, '0') || '1'
            WHEN 'commercial paper' THEN 'INE' || lpad((g.i % 800 + 200)::text, 3, '0') || 'A14' || lpad((g.i % 90 + 10)::text, 2, '0') || '2'
            WHEN 'government securities' THEN 'IN' || lpad((10 + (g.i % 25))::text, 2, '0') || '2023' || lpad((g.i % 9000 + 1000)::text, 4, '0') || '8'
            WHEN 'municipal bond' THEN 'INE0' || lpad((10 + (g.i % 40))::text, 2, '0') || 'M07' || lpad((g.i % 90 + 10)::text, 2, '0') || '5'
            WHEN 'mortgage backed securities' THEN 'INE99' || (g.i % 9 + 1) || 'M08' || lpad((g.i % 90 + 10)::text, 2, '0') || '7'
            WHEN 'bond etf' THEN 'INF' || lpad((200 + (g.i % 550))::text, 3, '0') || 'K01' || lpad((g.i % 90 + 10)::text, 2, '0') || '3'
            ELSE 'IN0020' || (18 + (g.i % 6)) || '0' || lpad((g.i % 900 + 100)::text, 3, '0') || 'G'
        END AS isin_code,
        CASE g.cat
            WHEN 'Government bond' THEN 100.00
            WHEN 'Treasury bill' THEN 100.00
            WHEN 'Corporate bond' THEN 1000.00
            WHEN 'commercial paper' THEN 500000.00
            WHEN 'government securities' THEN 100.00
            WHEN 'municipal bond' THEN 1000.00
            WHEN 'mortgage backed securities' THEN 10000.00
            WHEN 'bond etf' THEN 1000.00
            ELSE 5000.00
        END AS fv,
        CASE g.cat
            WHEN 'Government bond' THEN 10000 + (abs(hashtext(g.i::text || 'lrg_gb_q')) % 4990000)::bigint
            WHEN 'Treasury bill' THEN 50000 + (abs(hashtext(g.i::text || 'lrg_tb_q')) % 9950000)::bigint
            WHEN 'Corporate bond' THEN 5000 + (abs(hashtext(g.i::text || 'lrg_cb_q')) % 495000)::bigint
            WHEN 'commercial paper' THEN 10 + (abs(hashtext(g.i::text || 'lrg_cp_q')) % 1990)::bigint
            WHEN 'government securities' THEN 10000 + (abs(hashtext(g.i::text || 'lrg_gs_q')) % 4990000)::bigint
            WHEN 'municipal bond' THEN 2000 + (abs(hashtext(g.i::text || 'lrg_mb_q')) % 298000)::bigint
            WHEN 'mortgage backed securities' THEN 500 + (abs(hashtext(g.i::text || 'lrg_mbs_q')) % 49500)::bigint
            WHEN 'bond etf' THEN 10000 + (abs(hashtext(g.i::text || 'lrg_etf_q')) % 990000)::bigint
            ELSE 1000 + (abs(hashtext(g.i::text || 'lrg_gld_q')) % 199000)::bigint
        END AS qty,
        CASE g.cat
            WHEN 'Government bond' THEN round((6.75 + random() * 1.25)::numeric, 2)
            WHEN 'Treasury bill' THEN round((6.40 + random() * 0.70)::numeric, 2)
            WHEN 'Corporate bond' THEN round((7.75 + random() * 1.50)::numeric, 2)
            WHEN 'commercial paper' THEN round((7.10 + random() * 1.10)::numeric, 2)
            WHEN 'government securities' THEN round((7.20 + random() * 0.60)::numeric, 2)
            WHEN 'municipal bond' THEN round((8.15 + random() * 0.85)::numeric, 2)
            WHEN 'mortgage backed securities' THEN round((8.40 + random() * 1.20)::numeric, 2)
            WHEN 'bond etf' THEN round((6.90 + random() * 0.90)::numeric, 2)
            ELSE CASE WHEN (g.i % 5) <= 1 THEN 2.50 ELSE 0.00 END
        END AS coupon_yield,
        CASE g.cat
            WHEN 'Government bond' THEN CURRENT_DATE + ((365 * 2) + (g.i % 7000)) * interval '1 day'
            WHEN 'Treasury bill' THEN CURRENT_DATE + (CASE (g.i % 3) WHEN 0 THEN 15 + (g.i % 75) WHEN 1 THEN 90 + (g.i % 90) ELSE 180 + (g.i % 180) END) * interval '1 day'
            WHEN 'Corporate bond' THEN CURRENT_DATE + (365 + (g.i % 3650)) * interval '1 day'
            WHEN 'commercial paper' THEN CURRENT_DATE + (15 + (g.i % 250)) * interval '1 day'
            WHEN 'government securities' THEN CURRENT_DATE + (730 + (g.i % 4000)) * interval '1 day'
            WHEN 'municipal bond' THEN CURRENT_DATE + (365 * 2 + (g.i % 2500)) * interval '1 day'
            WHEN 'mortgage backed securities' THEN CURRENT_DATE + (365 * 3 + (g.i % 3000)) * interval '1 day'
            WHEN 'bond etf' THEN CURRENT_DATE + (365 * 2 + (g.i % 3000)) * interval '1 day'
            ELSE CASE WHEN (g.i % 5) <= 1 THEN CURRENT_DATE + (365 * 4 + (g.i % 1500)) * interval '1 day' ELSE NULL END
        END::date AS mat_date,
        CASE WHEN random() < 0.20 THEN 'Encumbered (Repo / Collateral)' ELSE 'Unencumbered' END AS encumb
    FROM gen g
)
SELECT
    'TRS-LRG-' || lpad(c.i::text, 5, '0') AS holding_id,
    c.isin_code AS isin,
    c.inst_name AS instrument_name,
    c.cat AS category,
    c.qty AS quantity,
    c.fv AS face_value_unit,
    round((c.qty * c.fv * CASE WHEN c.cat IN ('Treasury bill', 'commercial paper') THEN (0.94 + random() * 0.04) ELSE 1.00 END)::numeric, 2) AS book_value,
    round((c.qty * c.fv * (0.95 + random() * 0.09))::numeric, 2) AS market_value,
    c.encumb AS encumbrance_status,
    c.mat_date AS maturity_date,
    c.coupon_yield AS coupon_or_yield_pct,
    CURRENT_TIMESTAMP - ((c.i % 1440) * interval '1 minute') AS last_valuation_time
FROM calc c;

-- =========================================================================
-- 3. bank_medium (3,000 treasury holdings)
-- =========================================================================
INSERT INTO bank_medium.treasury_holdings (
    holding_id, isin, instrument_name, category, quantity, face_value_unit,
    book_value, market_value, encumbrance_status, maturity_date, coupon_or_yield_pct, last_valuation_time
)
WITH gen AS (
    SELECT 
        s.i,
        (ARRAY[
            'Government bond', 
            'Treasury bill', 
            'Corporate bond', 
            'commercial paper', 
            'government securities', 
            'municipal bond', 
            'mortgage backed securities', 
            'bond etf', 
            'gold commodities'
        ])[1 + ((abs(hashtext(s.i::text || 'med_cat')) + floor(random() * 9)::int) % 9)] AS cat
    FROM generate_series(1, 3000) AS s(i)
),
calc AS (
    SELECT
        g.i,
        g.cat,
        CASE g.cat
            WHEN 'Government bond' THEN (ARRAY['7.18% GS 2033 Benchmark', '7.26% GS 2032', '7.06% GS 2028', '7.38% GS 2027', '6.54% GS 2032', '7.10% GS 2034', '7.40% GS 2035', '6.99% GS 2051', '7.54% GS 2036', '7.17% GS 2028'])[1 + (g.i % 10)]
            WHEN 'Treasury bill' THEN CASE (g.i % 3) WHEN 0 THEN '91-Day T-Bill Series-' || (g.i % 40 + 1) WHEN 1 THEN '182-Day T-Bill Series-' || (g.i % 40 + 1) ELSE '364-Day T-Bill Series-' || (g.i % 40 + 1) END
            WHEN 'Corporate bond' THEN (ARRAY['8.15% HDFC Bank AAA Bond 2029', '7.95% ICICI Bank Tier-2 2031', '8.40% REC Limited Bond 2028', '8.20% PFC Infrastructure Bond 2030', '7.85% Bajaj Finance Senior NCD 2027', '8.50% L&T Finance Debenture 2029', '8.65% Tata Capital Bond 2028', '8.10% NTPC PSU Bond 2032'])[1 + (g.i % 8)]
            WHEN 'commercial paper' THEN (ARRAY['Reliance Retail CP 90D', 'Tata Motors Finance CP 180D', 'Adani Ports CP 90D', 'UltraTech Cement CP 60D', 'Mahindra & Mahindra Fin CP 120D', 'L&T Finance Holdings CP 90D'])[1 + (g.i % 6)]
            WHEN 'government securities' THEN (ARRAY['7.25% Sovereign Green Bond 2028', '7.30% SDL (Maharashtra) 2033', '7.42% SDL (Gujarat) 2031', '7.35% SDL (Tamil Nadu) 2032', '7.39% SDL (Karnataka) 2034', '7.45% SDL (Uttar Pradesh) 2033'])[1 + (g.i % 6)]
            WHEN 'municipal bond' THEN (ARRAY['8.50% Ahmedabad Municipal Green Bond 2029', '8.40% Pune Municipal Corp Bond 2028', '8.25% Vadodara Municipal Corp 2027', '8.60% Indore Municipal Green Bond 2030', '8.30% Surat Municipal Bond 2029'])[1 + (g.i % 5)]
            WHEN 'mortgage backed securities' THEN (ARRAY['HDFC Capital RMBS Tranche-A1', 'LIC Housing RMBS Trust Series-1', 'PNB Housing Prime MBS Class-A', 'Can Fin Homes Securitized Note 2028', 'Bajaj Housing Finance RMBS 2029'])[1 + (g.i % 5)]
            WHEN 'bond etf' THEN (ARRAY['Bharat Bond ETF - April 2030', 'Bharat Bond ETF - April 2032', 'Nippon India ETF Nifty CPSE Bond', 'Edelweiss Nifty PSU Bond Plus SDL ETF', 'Mirae Asset Nifty SDL June 2027 ETF'])[1 + (g.i % 5)]
            ELSE (ARRAY['Sovereign Gold Bond 2028 Series-VI', 'Sovereign Gold Bond 2029 Series-II', 'Nippon India Gold ETF Units', 'HDFC Gold ETF Holding', 'SBI Gold Sovereign Lot'])[1 + (g.i % 5)]
        END AS inst_name,
        CASE g.cat
            WHEN 'Government bond' THEN 'IN0020' || lpad((20 + (g.i % 10))::text, 2, '0') || lpad((g.i % 9000 + 1000)::text, 4, '0')
            WHEN 'Treasury bill' THEN 'IN002024' || (ARRAY['X', 'Y', 'Z'])[1 + (g.i % 3)] || lpad((g.i % 900 + 100)::text, 3, '0')
            WHEN 'Corporate bond' THEN 'INE' || lpad((g.i % 900 + 100)::text, 3, '0') || 'A08' || lpad((g.i % 90 + 10)::text, 2, '0') || '1'
            WHEN 'commercial paper' THEN 'INE' || lpad((g.i % 800 + 200)::text, 3, '0') || 'A14' || lpad((g.i % 90 + 10)::text, 2, '0') || '2'
            WHEN 'government securities' THEN 'IN' || lpad((10 + (g.i % 25))::text, 2, '0') || '2023' || lpad((g.i % 9000 + 1000)::text, 4, '0') || '8'
            WHEN 'municipal bond' THEN 'INE0' || lpad((10 + (g.i % 40))::text, 2, '0') || 'M07' || lpad((g.i % 90 + 10)::text, 2, '0') || '5'
            WHEN 'mortgage backed securities' THEN 'INE99' || (g.i % 9 + 1) || 'M08' || lpad((g.i % 90 + 10)::text, 2, '0') || '7'
            WHEN 'bond etf' THEN 'INF' || lpad((200 + (g.i % 550))::text, 3, '0') || 'K01' || lpad((g.i % 90 + 10)::text, 2, '0') || '3'
            ELSE 'IN0020' || (18 + (g.i % 6)) || '0' || lpad((g.i % 900 + 100)::text, 3, '0') || 'G'
        END AS isin_code,
        CASE g.cat
            WHEN 'Government bond' THEN 100.00
            WHEN 'Treasury bill' THEN 100.00
            WHEN 'Corporate bond' THEN 1000.00
            WHEN 'commercial paper' THEN 500000.00
            WHEN 'government securities' THEN 100.00
            WHEN 'municipal bond' THEN 1000.00
            WHEN 'mortgage backed securities' THEN 10000.00
            WHEN 'bond etf' THEN 1000.00
            ELSE 5000.00
        END AS fv,
        CASE g.cat
            WHEN 'Government bond' THEN 8000 + (abs(hashtext(g.i::text || 'med_gb_q')) % 3490000)::bigint
            WHEN 'Treasury bill' THEN 30000 + (abs(hashtext(g.i::text || 'med_tb_q')) % 6950000)::bigint
            WHEN 'Corporate bond' THEN 3000 + (abs(hashtext(g.i::text || 'med_cb_q')) % 345000)::bigint
            WHEN 'commercial paper' THEN 8 + (abs(hashtext(g.i::text || 'med_cp_q')) % 1490)::bigint
            WHEN 'government securities' THEN 8000 + (abs(hashtext(g.i::text || 'med_gs_q')) % 3490000)::bigint
            WHEN 'municipal bond' THEN 1500 + (abs(hashtext(g.i::text || 'med_mb_q')) % 198000)::bigint
            WHEN 'mortgage backed securities' THEN 300 + (abs(hashtext(g.i::text || 'med_mbs_q')) % 34500)::bigint
            WHEN 'bond etf' THEN 8000 + (abs(hashtext(g.i::text || 'med_etf_q')) % 690000)::bigint
            ELSE 800 + (abs(hashtext(g.i::text || 'med_gld_q')) % 149000)::bigint
        END AS qty,
        CASE g.cat
            WHEN 'Government bond' THEN round((6.80 + random() * 1.20)::numeric, 2)
            WHEN 'Treasury bill' THEN round((6.45 + random() * 0.70)::numeric, 2)
            WHEN 'Corporate bond' THEN round((7.80 + random() * 1.50)::numeric, 2)
            WHEN 'commercial paper' THEN round((7.15 + random() * 1.10)::numeric, 2)
            WHEN 'government securities' THEN round((7.25 + random() * 0.60)::numeric, 2)
            WHEN 'municipal bond' THEN round((8.20 + random() * 0.85)::numeric, 2)
            WHEN 'mortgage backed securities' THEN round((8.45 + random() * 1.20)::numeric, 2)
            WHEN 'bond etf' THEN round((6.95 + random() * 0.90)::numeric, 2)
            ELSE CASE WHEN (g.i % 5) <= 1 THEN 2.50 ELSE 0.00 END
        END AS coupon_yield,
        CASE g.cat
            WHEN 'Government bond' THEN CURRENT_DATE + ((365 * 2) + (g.i % 6000)) * interval '1 day'
            WHEN 'Treasury bill' THEN CURRENT_DATE + (CASE (g.i % 3) WHEN 0 THEN 15 + (g.i % 75) WHEN 1 THEN 90 + (g.i % 90) ELSE 180 + (g.i % 180) END) * interval '1 day'
            WHEN 'Corporate bond' THEN CURRENT_DATE + (365 + (g.i % 3000)) * interval '1 day'
            WHEN 'commercial paper' THEN CURRENT_DATE + (15 + (g.i % 250)) * interval '1 day'
            WHEN 'government securities' THEN CURRENT_DATE + (730 + (g.i % 3500)) * interval '1 day'
            WHEN 'municipal bond' THEN CURRENT_DATE + (365 * 2 + (g.i % 2200)) * interval '1 day'
            WHEN 'mortgage backed securities' THEN CURRENT_DATE + (365 * 3 + (g.i % 2800)) * interval '1 day'
            WHEN 'bond etf' THEN CURRENT_DATE + (365 * 2 + (g.i % 2800)) * interval '1 day'
            ELSE CASE WHEN (g.i % 5) <= 1 THEN CURRENT_DATE + (365 * 4 + (g.i % 1400)) * interval '1 day' ELSE NULL END
        END::date AS mat_date,
        CASE WHEN random() < 0.22 THEN 'Encumbered (Repo / Collateral)' ELSE 'Unencumbered' END AS encumb
    FROM gen g
)
SELECT
    'TRS-MED-' || lpad(c.i::text, 5, '0') AS holding_id,
    c.isin_code AS isin,
    c.inst_name AS instrument_name,
    c.cat AS category,
    c.qty AS quantity,
    c.fv AS face_value_unit,
    round((c.qty * c.fv * CASE WHEN c.cat IN ('Treasury bill', 'commercial paper') THEN (0.94 + random() * 0.04) ELSE 1.00 END)::numeric, 2) AS book_value,
    round((c.qty * c.fv * (0.95 + random() * 0.09))::numeric, 2) AS market_value,
    c.encumb AS encumbrance_status,
    c.mat_date AS maturity_date,
    c.coupon_yield AS coupon_or_yield_pct,
    CURRENT_TIMESTAMP - ((c.i % 1440) * interval '1 minute') AS last_valuation_time
FROM calc c;

-- =========================================================================
-- 4. bank_small (2,500 treasury holdings)
-- =========================================================================
INSERT INTO bank_small.treasury_holdings (
    holding_id, isin, instrument_name, category, quantity, face_value_unit,
    book_value, market_value, encumbrance_status, maturity_date, coupon_or_yield_pct, last_valuation_time
)
WITH gen AS (
    SELECT 
        s.i,
        (ARRAY[
            'Government bond', 
            'Treasury bill', 
            'Corporate bond', 
            'commercial paper', 
            'government securities', 
            'municipal bond', 
            'mortgage backed securities', 
            'bond etf', 
            'gold commodities'
        ])[1 + ((abs(hashtext(s.i::text || 'sml_cat')) + floor(random() * 9)::int) % 9)] AS cat
    FROM generate_series(1, 2500) AS s(i)
),
calc AS (
    SELECT
        g.i,
        g.cat,
        CASE g.cat
            WHEN 'Government bond' THEN (ARRAY['7.18% GS 2033 Benchmark', '7.26% GS 2032', '7.06% GS 2028', '7.38% GS 2027', '6.54% GS 2032', '7.10% GS 2034', '7.40% GS 2035', '6.99% GS 2051', '7.54% GS 2036', '7.17% GS 2028'])[1 + (g.i % 10)]
            WHEN 'Treasury bill' THEN CASE (g.i % 3) WHEN 0 THEN '91-Day T-Bill Series-' || (g.i % 40 + 1) WHEN 1 THEN '182-Day T-Bill Series-' || (g.i % 40 + 1) ELSE '364-Day T-Bill Series-' || (g.i % 40 + 1) END
            WHEN 'Corporate bond' THEN (ARRAY['8.15% HDFC Bank AAA Bond 2029', '7.95% ICICI Bank Tier-2 2031', '8.40% REC Limited Bond 2028', '8.20% PFC Infrastructure Bond 2030', '7.85% Bajaj Finance Senior NCD 2027', '8.50% L&T Finance Debenture 2029', '8.65% Tata Capital Bond 2028', '8.10% NTPC PSU Bond 2032'])[1 + (g.i % 8)]
            WHEN 'commercial paper' THEN (ARRAY['Reliance Retail CP 90D', 'Tata Motors Finance CP 180D', 'Adani Ports CP 90D', 'UltraTech Cement CP 60D', 'Mahindra & Mahindra Fin CP 120D', 'L&T Finance Holdings CP 90D'])[1 + (g.i % 6)]
            WHEN 'government securities' THEN (ARRAY['7.25% Sovereign Green Bond 2028', '7.30% SDL (Maharashtra) 2033', '7.42% SDL (Gujarat) 2031', '7.35% SDL (Tamil Nadu) 2032', '7.39% SDL (Karnataka) 2034', '7.45% SDL (Uttar Pradesh) 2033'])[1 + (g.i % 6)]
            WHEN 'municipal bond' THEN (ARRAY['8.50% Ahmedabad Municipal Green Bond 2029', '8.40% Pune Municipal Corp Bond 2028', '8.25% Vadodara Municipal Corp 2027', '8.60% Indore Municipal Green Bond 2030', '8.30% Surat Municipal Bond 2029'])[1 + (g.i % 5)]
            WHEN 'mortgage backed securities' THEN (ARRAY['HDFC Capital RMBS Tranche-A1', 'LIC Housing RMBS Trust Series-1', 'PNB Housing Prime MBS Class-A', 'Can Fin Homes Securitized Note 2028', 'Bajaj Housing Finance RMBS 2029'])[1 + (g.i % 5)]
            WHEN 'bond etf' THEN (ARRAY['Bharat Bond ETF - April 2030', 'Bharat Bond ETF - April 2032', 'Nippon India ETF Nifty CPSE Bond', 'Edelweiss Nifty PSU Bond Plus SDL ETF', 'Mirae Asset Nifty SDL June 2027 ETF'])[1 + (g.i % 5)]
            ELSE (ARRAY['Sovereign Gold Bond 2028 Series-VI', 'Sovereign Gold Bond 2029 Series-II', 'Nippon India Gold ETF Units', 'HDFC Gold ETF Holding', 'SBI Gold Sovereign Lot'])[1 + (g.i % 5)]
        END AS inst_name,
        CASE g.cat
            WHEN 'Government bond' THEN 'IN0020' || lpad((20 + (g.i % 10))::text, 2, '0') || lpad((g.i % 9000 + 1000)::text, 4, '0')
            WHEN 'Treasury bill' THEN 'IN002024' || (ARRAY['X', 'Y', 'Z'])[1 + (g.i % 3)] || lpad((g.i % 900 + 100)::text, 3, '0')
            WHEN 'Corporate bond' THEN 'INE' || lpad((g.i % 900 + 100)::text, 3, '0') || 'A08' || lpad((g.i % 90 + 10)::text, 2, '0') || '1'
            WHEN 'commercial paper' THEN 'INE' || lpad((g.i % 800 + 200)::text, 3, '0') || 'A14' || lpad((g.i % 90 + 10)::text, 2, '0') || '2'
            WHEN 'government securities' THEN 'IN' || lpad((10 + (g.i % 25))::text, 2, '0') || '2023' || lpad((g.i % 9000 + 1000)::text, 4, '0') || '8'
            WHEN 'municipal bond' THEN 'INE0' || lpad((10 + (g.i % 40))::text, 2, '0') || 'M07' || lpad((g.i % 90 + 10)::text, 2, '0') || '5'
            WHEN 'mortgage backed securities' THEN 'INE99' || (g.i % 9 + 1) || 'M08' || lpad((g.i % 90 + 10)::text, 2, '0') || '7'
            WHEN 'bond etf' THEN 'INF' || lpad((200 + (g.i % 550))::text, 3, '0') || 'K01' || lpad((g.i % 90 + 10)::text, 2, '0') || '3'
            ELSE 'IN0020' || (18 + (g.i % 6)) || '0' || lpad((g.i % 900 + 100)::text, 3, '0') || 'G'
        END AS isin_code,
        CASE g.cat
            WHEN 'Government bond' THEN 100.00
            WHEN 'Treasury bill' THEN 100.00
            WHEN 'Corporate bond' THEN 1000.00
            WHEN 'commercial paper' THEN 500000.00
            WHEN 'government securities' THEN 100.00
            WHEN 'municipal bond' THEN 1000.00
            WHEN 'mortgage backed securities' THEN 10000.00
            WHEN 'bond etf' THEN 1000.00
            ELSE 5000.00
        END AS fv,
        CASE g.cat
            WHEN 'Government bond' THEN 5000 + (abs(hashtext(g.i::text || 'sml_gb_q')) % 1990000)::bigint
            WHEN 'Treasury bill' THEN 20000 + (abs(hashtext(g.i::text || 'sml_tb_q')) % 3950000)::bigint
            WHEN 'Corporate bond' THEN 2000 + (abs(hashtext(g.i::text || 'sml_cb_q')) % 195000)::bigint
            WHEN 'commercial paper' THEN 5 + (abs(hashtext(g.i::text || 'sml_cp_q')) % 990)::bigint
            WHEN 'government securities' THEN 5000 + (abs(hashtext(g.i::text || 'sml_gs_q')) % 1990000)::bigint
            WHEN 'municipal bond' THEN 1000 + (abs(hashtext(g.i::text || 'sml_mb_q')) % 98000)::bigint
            WHEN 'mortgage backed securities' THEN 200 + (abs(hashtext(g.i::text || 'sml_mbs_q')) % 19500)::bigint
            WHEN 'bond etf' THEN 5000 + (abs(hashtext(g.i::text || 'sml_etf_q')) % 390000)::bigint
            ELSE 500 + (abs(hashtext(g.i::text || 'sml_gld_q')) % 99000)::bigint
        END AS qty,
        CASE g.cat
            WHEN 'Government bond' THEN round((6.85 + random() * 1.15)::numeric, 2)
            WHEN 'Treasury bill' THEN round((6.50 + random() * 0.70)::numeric, 2)
            WHEN 'Corporate bond' THEN round((7.85 + random() * 1.50)::numeric, 2)
            WHEN 'commercial paper' THEN round((7.20 + random() * 1.10)::numeric, 2)
            WHEN 'government securities' THEN round((7.30 + random() * 0.60)::numeric, 2)
            WHEN 'municipal bond' THEN round((8.25 + random() * 0.85)::numeric, 2)
            WHEN 'mortgage backed securities' THEN round((8.50 + random() * 1.20)::numeric, 2)
            WHEN 'bond etf' THEN round((7.00 + random() * 0.90)::numeric, 2)
            ELSE CASE WHEN (g.i % 5) <= 1 THEN 2.50 ELSE 0.00 END
        END AS coupon_yield,
        CASE g.cat
            WHEN 'Government bond' THEN CURRENT_DATE + ((365 * 2) + (g.i % 5000)) * interval '1 day'
            WHEN 'Treasury bill' THEN CURRENT_DATE + (CASE (g.i % 3) WHEN 0 THEN 15 + (g.i % 75) WHEN 1 THEN 90 + (g.i % 90) ELSE 180 + (g.i % 180) END) * interval '1 day'
            WHEN 'Corporate bond' THEN CURRENT_DATE + (365 + (g.i % 2500)) * interval '1 day'
            WHEN 'commercial paper' THEN CURRENT_DATE + (15 + (g.i % 250)) * interval '1 day'
            WHEN 'government securities' THEN CURRENT_DATE + (730 + (g.i % 3000)) * interval '1 day'
            WHEN 'municipal bond' THEN CURRENT_DATE + (365 * 2 + (g.i % 1800)) * interval '1 day'
            WHEN 'mortgage backed securities' THEN CURRENT_DATE + (365 * 3 + (g.i % 2500)) * interval '1 day'
            WHEN 'bond etf' THEN CURRENT_DATE + (365 * 2 + (g.i % 2500)) * interval '1 day'
            ELSE CASE WHEN (g.i % 5) <= 1 THEN CURRENT_DATE + (365 * 4 + (g.i % 1200)) * interval '1 day' ELSE NULL END
        END::date AS mat_date,
        CASE WHEN random() < 0.25 THEN 'Encumbered (Repo / Collateral)' ELSE 'Unencumbered' END AS encumb
    FROM gen g
)
SELECT
    'TRS-SML-' || lpad(c.i::text, 5, '0') AS holding_id,
    c.isin_code AS isin,
    c.inst_name AS instrument_name,
    c.cat AS category,
    c.qty AS quantity,
    c.fv AS face_value_unit,
    round((c.qty * c.fv * CASE WHEN c.cat IN ('Treasury bill', 'commercial paper') THEN (0.94 + random() * 0.04) ELSE 1.00 END)::numeric, 2) AS book_value,
    round((c.qty * c.fv * (0.95 + random() * 0.09))::numeric, 2) AS market_value,
    c.encumb AS encumbrance_status,
    c.mat_date AS maturity_date,
    c.coupon_yield AS coupon_or_yield_pct,
    CURRENT_TIMESTAMP - ((c.i % 1440) * interval '1 minute') AS last_valuation_time
FROM calc c;

-- =========================================================================
-- 5. bank_distressed (1,500 treasury holdings, higher encumbered share)
-- =========================================================================
INSERT INTO bank_distressed.treasury_holdings (
    holding_id, isin, instrument_name, category, quantity, face_value_unit,
    book_value, market_value, encumbrance_status, maturity_date, coupon_or_yield_pct, last_valuation_time
)
WITH gen AS (
    SELECT 
        s.i,
        (ARRAY[
            'Government bond', 
            'Treasury bill', 
            'Corporate bond', 
            'commercial paper', 
            'government securities', 
            'municipal bond', 
            'mortgage backed securities', 
            'bond etf', 
            'gold commodities'
        ])[1 + ((abs(hashtext(s.i::text || 'dst_cat')) + floor(random() * 9)::int) % 9)] AS cat
    FROM generate_series(1, 1500) AS s(i)
),
calc AS (
    SELECT
        g.i,
        g.cat,
        CASE g.cat
            WHEN 'Government bond' THEN (ARRAY['7.18% GS 2033 Benchmark', '7.26% GS 2032', '7.06% GS 2028', '7.38% GS 2027', '6.54% GS 2032', '7.10% GS 2034', '7.40% GS 2035', '6.99% GS 2051', '7.54% GS 2036', '7.17% GS 2028'])[1 + (g.i % 10)]
            WHEN 'Treasury bill' THEN CASE (g.i % 3) WHEN 0 THEN '91-Day T-Bill Series-' || (g.i % 40 + 1) WHEN 1 THEN '182-Day T-Bill Series-' || (g.i % 40 + 1) ELSE '364-Day T-Bill Series-' || (g.i % 40 + 1) END
            WHEN 'Corporate bond' THEN (ARRAY['8.15% HDFC Bank AAA Bond 2029', '7.95% ICICI Bank Tier-2 2031', '8.40% REC Limited Bond 2028', '8.20% PFC Infrastructure Bond 2030', '7.85% Bajaj Finance Senior NCD 2027', '8.50% L&T Finance Debenture 2029', '8.65% Tata Capital Bond 2028', '8.10% NTPC PSU Bond 2032'])[1 + (g.i % 8)]
            WHEN 'commercial paper' THEN (ARRAY['Reliance Retail CP 90D', 'Tata Motors Finance CP 180D', 'Adani Ports CP 90D', 'UltraTech Cement CP 60D', 'Mahindra & Mahindra Fin CP 120D', 'L&T Finance Holdings CP 90D'])[1 + (g.i % 6)]
            WHEN 'government securities' THEN (ARRAY['7.25% Sovereign Green Bond 2028', '7.30% SDL (Maharashtra) 2033', '7.42% SDL (Gujarat) 2031', '7.35% SDL (Tamil Nadu) 2032', '7.39% SDL (Karnataka) 2034', '7.45% SDL (Uttar Pradesh) 2033'])[1 + (g.i % 6)]
            WHEN 'municipal bond' THEN (ARRAY['8.50% Ahmedabad Municipal Green Bond 2029', '8.40% Pune Municipal Corp Bond 2028', '8.25% Vadodara Municipal Corp 2027', '8.60% Indore Municipal Green Bond 2030', '8.30% Surat Municipal Bond 2029'])[1 + (g.i % 5)]
            WHEN 'mortgage backed securities' THEN (ARRAY['HDFC Capital RMBS Tranche-A1', 'LIC Housing RMBS Trust Series-1', 'PNB Housing Prime MBS Class-A', 'Can Fin Homes Securitized Note 2028', 'Bajaj Housing Finance RMBS 2029'])[1 + (g.i % 5)]
            WHEN 'bond etf' THEN (ARRAY['Bharat Bond ETF - April 2030', 'Bharat Bond ETF - April 2032', 'Nippon India ETF Nifty CPSE Bond', 'Edelweiss Nifty PSU Bond Plus SDL ETF', 'Mirae Asset Nifty SDL June 2027 ETF'])[1 + (g.i % 5)]
            ELSE (ARRAY['Sovereign Gold Bond 2028 Series-VI', 'Sovereign Gold Bond 2029 Series-II', 'Nippon India Gold ETF Units', 'HDFC Gold ETF Holding', 'SBI Gold Sovereign Lot'])[1 + (g.i % 5)]
        END AS inst_name,
        CASE g.cat
            WHEN 'Government bond' THEN 'IN0020' || lpad((20 + (g.i % 10))::text, 2, '0') || lpad((g.i % 9000 + 1000)::text, 4, '0')
            WHEN 'Treasury bill' THEN 'IN002024' || (ARRAY['X', 'Y', 'Z'])[1 + (g.i % 3)] || lpad((g.i % 900 + 100)::text, 3, '0')
            WHEN 'Corporate bond' THEN 'INE' || lpad((g.i % 900 + 100)::text, 3, '0') || 'A08' || lpad((g.i % 90 + 10)::text, 2, '0') || '1'
            WHEN 'commercial paper' THEN 'INE' || lpad((g.i % 800 + 200)::text, 3, '0') || 'A14' || lpad((g.i % 90 + 10)::text, 2, '0') || '2'
            WHEN 'government securities' THEN 'IN' || lpad((10 + (g.i % 25))::text, 2, '0') || '2023' || lpad((g.i % 9000 + 1000)::text, 4, '0') || '8'
            WHEN 'municipal bond' THEN 'INE0' || lpad((10 + (g.i % 40))::text, 2, '0') || 'M07' || lpad((g.i % 90 + 10)::text, 2, '0') || '5'
            WHEN 'mortgage backed securities' THEN 'INE99' || (g.i % 9 + 1) || 'M08' || lpad((g.i % 90 + 10)::text, 2, '0') || '7'
            WHEN 'bond etf' THEN 'INF' || lpad((200 + (g.i % 550))::text, 3, '0') || 'K01' || lpad((g.i % 90 + 10)::text, 2, '0') || '3'
            ELSE 'IN0020' || (18 + (g.i % 6)) || '0' || lpad((g.i % 900 + 100)::text, 3, '0') || 'G'
        END AS isin_code,
        CASE g.cat
            WHEN 'Government bond' THEN 100.00
            WHEN 'Treasury bill' THEN 100.00
            WHEN 'Corporate bond' THEN 1000.00
            WHEN 'commercial paper' THEN 500000.00
            WHEN 'government securities' THEN 100.00
            WHEN 'municipal bond' THEN 1000.00
            WHEN 'mortgage backed securities' THEN 10000.00
            WHEN 'bond etf' THEN 1000.00
            ELSE 5000.00
        END AS fv,
        CASE g.cat
            WHEN 'Government bond' THEN 3000 + (abs(hashtext(g.i::text || 'dst_gb_q')) % 990000)::bigint
            WHEN 'Treasury bill' THEN 10000 + (abs(hashtext(g.i::text || 'dst_tb_q')) % 1950000)::bigint
            WHEN 'Corporate bond' THEN 1000 + (abs(hashtext(g.i::text || 'dst_cb_q')) % 95000)::bigint
            WHEN 'commercial paper' THEN 3 + (abs(hashtext(g.i::text || 'dst_cp_q')) % 490)::bigint
            WHEN 'government securities' THEN 3000 + (abs(hashtext(g.i::text || 'dst_gs_q')) % 990000)::bigint
            WHEN 'municipal bond' THEN 500 + (abs(hashtext(g.i::text || 'dst_mb_q')) % 48000)::bigint
            WHEN 'mortgage backed securities' THEN 100 + (abs(hashtext(g.i::text || 'dst_mbs_q')) % 9500)::bigint
            WHEN 'bond etf' THEN 3000 + (abs(hashtext(g.i::text || 'dst_etf_q')) % 190000)::bigint
            ELSE 300 + (abs(hashtext(g.i::text || 'dst_gld_q')) % 49000)::bigint
        END AS qty,
        CASE g.cat
            WHEN 'Government bond' THEN round((6.90 + random() * 1.20)::numeric, 2)
            WHEN 'Treasury bill' THEN round((6.55 + random() * 0.70)::numeric, 2)
            WHEN 'Corporate bond' THEN round((8.20 + random() * 1.80)::numeric, 2)
            WHEN 'commercial paper' THEN round((7.50 + random() * 1.30)::numeric, 2)
            WHEN 'government securities' THEN round((7.35 + random() * 0.65)::numeric, 2)
            WHEN 'municipal bond' THEN round((8.40 + random() * 0.95)::numeric, 2)
            WHEN 'mortgage backed securities' THEN round((8.80 + random() * 1.40)::numeric, 2)
            WHEN 'bond etf' THEN round((7.10 + random() * 0.90)::numeric, 2)
            ELSE CASE WHEN (g.i % 5) <= 1 THEN 2.50 ELSE 0.00 END
        END AS coupon_yield,
        CASE g.cat
            WHEN 'Government bond' THEN CURRENT_DATE + ((365 * 2) + (g.i % 4000)) * interval '1 day'
            WHEN 'Treasury bill' THEN CURRENT_DATE + (CASE (g.i % 3) WHEN 0 THEN 15 + (g.i % 75) WHEN 1 THEN 90 + (g.i % 90) ELSE 180 + (g.i % 180) END) * interval '1 day'
            WHEN 'Corporate bond' THEN CURRENT_DATE + (365 + (g.i % 2000)) * interval '1 day'
            WHEN 'commercial paper' THEN CURRENT_DATE + (15 + (g.i % 250)) * interval '1 day'
            WHEN 'government securities' THEN CURRENT_DATE + (730 + (g.i % 2500)) * interval '1 day'
            WHEN 'municipal bond' THEN CURRENT_DATE + (365 * 2 + (g.i % 1500)) * interval '1 day'
            WHEN 'mortgage backed securities' THEN CURRENT_DATE + (365 * 3 + (g.i % 2000)) * interval '1 day'
            WHEN 'bond etf' THEN CURRENT_DATE + (365 * 2 + (g.i % 2000)) * interval '1 day'
            ELSE CASE WHEN (g.i % 5) <= 1 THEN CURRENT_DATE + (365 * 4 + (g.i % 1000)) * interval '1 day' ELSE NULL END
        END::date AS mat_date,
        -- Distressed bank has 55% encumbered portfolio pledged for emergency liquidity/repo
        CASE WHEN random() < 0.55 THEN 'Encumbered (Repo / Collateral)' ELSE 'Unencumbered' END AS encumb
    FROM gen g
)
SELECT
    'TRS-DST-' || lpad(c.i::text, 5, '0') AS holding_id,
    c.isin_code AS isin,
    c.inst_name AS instrument_name,
    c.cat AS category,
    c.qty AS quantity,
    c.fv AS face_value_unit,
    round((c.qty * c.fv * CASE WHEN c.cat IN ('Treasury bill', 'commercial paper') THEN (0.94 + random() * 0.04) ELSE 1.00 END)::numeric, 2) AS book_value,
    round((c.qty * c.fv * (0.95 + random() * 0.09))::numeric, 2) AS market_value,
    c.encumb AS encumbrance_status,
    c.mat_date AS maturity_date,
    c.coupon_yield AS coupon_or_yield_pct,
    CURRENT_TIMESTAMP - ((c.i % 1440) * interval '1 minute') AS last_valuation_time
FROM calc c;
