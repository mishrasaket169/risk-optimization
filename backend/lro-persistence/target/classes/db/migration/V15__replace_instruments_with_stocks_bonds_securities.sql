-- V15: Replace legacy exchange.instruments with dedicated exchange.stocks, exchange.bonds, and exchange.securities tables
-- Drop legacy foreign key constraints and instruments table
ALTER TABLE IF EXISTS exchange.orders DROP CONSTRAINT IF EXISTS orders_symbol_fkey;
ALTER TABLE IF EXISTS exchange.orders DROP CONSTRAINT IF EXISTS exchange_orders_symbol_fkey;
DROP TABLE IF EXISTS exchange.instruments CASCADE;

-- 1. Create exchange.stocks Table
CREATE TABLE IF NOT EXISTS exchange.stocks (
    symbol VARCHAR(32) PRIMARY KEY,
    company_name VARCHAR(128) NOT NULL,
    sector VARCHAR(64) NOT NULL,
    ltp NUMERIC(18, 4) NOT NULL,
    change_pct NUMERIC(8, 4) NOT NULL DEFAULT 0.00,
    bid_price NUMERIC(18, 4) NOT NULL,
    ask_price NUMERIC(18, 4) NOT NULL,
    volume BIGINT NOT NULL DEFAULT 0,
    turnover NUMERIC(18, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. Create exchange.bonds Table
CREATE TABLE IF NOT EXISTS exchange.bonds (
    bond_symbol VARCHAR(32) PRIMARY KEY,
    issuer_series VARCHAR(128) NOT NULL,
    sector_type VARCHAR(64) NOT NULL,
    coupon_rate_pct NUMERIC(6, 2) NOT NULL,
    clean_price NUMERIC(18, 4) NOT NULL,
    change_pct NUMERIC(8, 4) NOT NULL DEFAULT 0.00,
    bid NUMERIC(18, 4) NOT NULL,
    ask NUMERIC(18, 4) NOT NULL,
    volume BIGINT NOT NULL DEFAULT 0,
    maturity_date DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 3. Create exchange.securities Table
CREATE TABLE IF NOT EXISTS exchange.securities (
    security_symbol VARCHAR(32) PRIMARY KEY,
    security_description VARCHAR(128) NOT NULL,
    category VARCHAR(64) NOT NULL,
    benchmark_yield_pct NUMERIC(6, 2) NOT NULL,
    clean_price NUMERIC(18, 4) NOT NULL,
    change_pct NUMERIC(8, 4) NOT NULL DEFAULT 0.00,
    bid NUMERIC(18, 4) NOT NULL,
    ask NUMERIC(18, 4) NOT NULL,
    volume BIGINT NOT NULL DEFAULT 0,
    maturity_date DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Seed Canonical 10 Stocks from Screenshot
INSERT INTO exchange.stocks (symbol, company_name, sector, ltp, change_pct, bid_price, ask_price, volume, turnover)
VALUES
('RELIANCE', 'Reliance Industries Limited', 'Energy / Conglomerate', 2840.50, 1.20, 2840.25, 2840.75, 1845000, 5240000000.00),
('HDFCBANK', 'HDFC Bank Limited', 'Banking & Financials', 1685.20, 0.85, 1685.00, 1685.50, 2450000, 4128000000.00),
('INFY', 'Infosys Limited', 'Information Technology', 1820.75, -0.45, 1820.50, 1821.00, 1230000, 2239000000.00),
('TCS', 'Tata Consultancy Services Ltd', 'Information Technology', 4150.00, 0.30, 4149.50, 4150.50, 820000, 3403000000.00),
('ICICIBANK', 'ICICI Bank Limited', 'Banking & Financials', 1210.40, 1.45, 1210.10, 1210.60, 3120000, 3776000000.00),
('SBIN', 'State Bank of India', 'Public Sector Banking', 815.60, -1.10, 815.30, 815.80, 4200000, 3425500000.00),
('BHARTIARTL', 'Bharti Airtel Limited', 'Telecommunications', 1540.25, -0.65, 1540.00, 1540.50, 1650000, 2541400000.00),
('LT', 'Larsen & Toubro Limited', 'Infrastructure & Capital Goods', 3560.00, -0.20, 3559.00, 3561.00, 920000, 3275200000.00),
('TATAMOTORS', 'Tata Motors Limited', 'Automotive', 980.50, 2.15, 980.20, 980.80, 3400000, 3333700000.00),
('ITC', 'ITC Limited', 'FMCG / Diversified', 495.30, 0.40, 495.10, 495.50, 2800000, 1386800000.00)
ON CONFLICT (symbol) DO NOTHING;

-- Seed 1,000 Additional Stocks
DO $$
DECLARE
    i INT;
    v_sym VARCHAR(32);
    v_name VARCHAR(128);
    v_sec VARCHAR(64);
    v_ltp NUMERIC(18, 4);
    v_chg NUMERIC(8, 4);
    v_bid NUMERIC(18, 4);
    v_ask NUMERIC(18, 4);
    v_vol BIGINT;
    v_turn NUMERIC(18, 2);
    v_names TEXT[] := ARRAY[
        'Adani Enterprises Ltd', 'Kotak Mahindra Bank', 'Axis Bank Limited', 'Bajaj Finance Limited', 'Wipro Limited',
        'Sun Pharmaceutical Ind', 'Maruti Suzuki India', 'HCL Technologies Ltd', 'Asian Paints Limited', 'Titan Company Limited',
        'UltraTech Cement Ltd', 'NTPC Limited', 'Power Grid Corporation', 'Oil & Natural Gas Corp', 'Coal India Limited',
        'JSW Steel Limited', 'Grasim Industries Ltd', 'Hindalco Industries', 'Tech Mahindra Limited', 'Nestle India Limited',
        'Cipla Limited', 'Tata Steel Limited', 'Bharat Petroleum Corp', 'Eicher Motors Limited', 'Dr Reddys Laboratories',
        'Britannia Industries', 'Divis Laboratories Ltd', 'Apollo Hospitals Ent', 'Hero MotoCorp Limited', 'IndusInd Bank Limited',
        'Bajaj Finserv Limited', 'Shree Cement Limited', 'Pidilite Industries', 'Havells India Limited', 'Dabur India Limited',
        'Godrej Consumer Products', 'Marico Limited', 'Voltas Limited', 'Trent Limited', 'Bharat Electronics Ltd',
        'Hindustan Aeronautics', 'Zomato Limited', 'One97 Communications', 'Avenue Supermarts Ltd', 'Polycab India Limited',
        'IRCTC Limited', 'Persistent Systems Ltd', 'Coforge Limited', 'Mphasis Limited', 'LTIMindtree Limited'
    ];
    v_symbols TEXT[] := ARRAY[
        'ADANIENT', 'KOTAKBANK', 'AXISBANK', 'BAJFINANCE', 'WIPRO',
        'SUNPHARMA', 'MARUTI', 'HCLTECH', 'ASIANPAINT', 'TITAN',
        'ULTRACEMCO', 'NTPC', 'POWERGRID', 'ONGC', 'COALINDIA',
        'JSWSTEEL', 'GRASIM', 'HINDALCO', 'TECHM', 'NESTLEIND',
        'CIPLA', 'TATASTEEL', 'BPCL', 'EICHERMOT', 'DRREDDY',
        'BRITANNIA', 'DIVISLAB', 'APOLLOHOSP', 'HEROMOTOCO', 'INDUSINDBK',
        'BAJAJFINSV', 'SHREECEM', 'PIDILITIND', 'HAVELLS', 'DABUR',
        'GODREJCP', 'MARICO', 'VOLTAS', 'TRENT', 'BEL',
        'HAL', 'ZOMATO', 'PAYTM', 'DMART', 'POLYCAB',
        'IRCTC', 'PERSISTENT', 'COFORGE', 'MPHASIS', 'LTIM'
    ];
    v_sectors TEXT[] := ARRAY[
        'Energy / Conglomerate', 'Banking & Financials', 'Banking & Financials', 'Banking & Financials', 'Information Technology',
        'Pharmaceuticals', 'Automotive', 'Information Technology', 'FMCG / Diversified', 'Consumer Goods',
        'Infrastructure & Capital Goods', 'Power & Utilities', 'Power & Utilities', 'Oil & Gas', 'Metals & Mining',
        'Metals & Mining', 'FMCG / Diversified', 'Metals & Mining', 'Information Technology', 'FMCG / Diversified',
        'Pharmaceuticals', 'Metals & Mining', 'Oil & Gas', 'Automotive', 'Pharmaceuticals',
        'FMCG / Diversified', 'Pharmaceuticals', 'Healthcare', 'Automotive', 'Banking & Financials',
        'Banking & Financials', 'Infrastructure & Capital Goods', 'Chemicals', 'Consumer Goods', 'FMCG / Diversified',
        'FMCG / Diversified', 'FMCG / Diversified', 'Consumer Goods', 'Retail', 'Defence & Aerospace',
        'Defence & Aerospace', 'Consumer Tech', 'Fintech', 'Retail', 'Infrastructure & Capital Goods',
        'Public Sector Services', 'Information Technology', 'Information Technology', 'Information Technology', 'Information Technology'
    ];
    v_idx INT;
    v_series INT;
BEGIN
    FOR i IN 1..1000 LOOP
        v_idx := ((i - 1) % 50) + 1;
        v_series := ((i - 1) / 50) + 1;
        
        IF v_series = 1 THEN
            v_sym := v_symbols[v_idx];
            v_name := v_names[v_idx];
        ELSE
            v_sym := v_symbols[v_idx] || '-S' || v_series;
            v_name := v_names[v_idx] || ' Series ' || v_series;
        END IF;
        
        v_sec := v_sectors[v_idx];
        v_ltp := ROUND((120.0 + (i * 23.47) % 4600.0)::numeric, 2);
        IF v_ltp < 25.0 THEN v_ltp := 250.00; END IF;
        
        v_chg := ROUND((((i * 17) % 700 - 320) / 100.0)::numeric, 2);
        v_bid := v_ltp - 0.25;
        v_ask := v_ltp + 0.25;
        v_vol := 45000 + ((i * 14200) % 5200000);
        v_turn := ROUND((v_vol * v_ltp)::numeric, 2);
        
        INSERT INTO exchange.stocks (symbol, company_name, sector, ltp, change_pct, bid_price, ask_price, volume, turnover)
        VALUES (v_sym, v_name, v_sec, v_ltp, v_chg, v_bid, v_ask, v_vol, v_turn)
        ON CONFLICT (symbol) DO NOTHING;
    END LOOP;
END $$;

-- Seed Canonical 6 Bonds from Screenshot
INSERT INTO exchange.bonds (bond_symbol, issuer_series, sector_type, coupon_rate_pct, clean_price, change_pct, bid, ask, volume, maturity_date)
VALUES
('HDFCB-8.15-29', '8.15% HDFC Bank AAA Bond 2029', 'Financial Services', 8.15, 998.00, -0.10, 997.50, 998.50, 150000, '2031-10-14'),
('REC-7.65-28', '7.65% Rural Electrification Corp AAA 2028', 'PSU / Infrastructure', 7.65, 1002.50, 0.05, 1002.00, 1003.00, 210000, '2030-10-14'),
('PFC-7.75-30', '7.75% Power Finance Corporation AAA 2030', 'Power & Utilities', 7.75, 1001.20, 0.08, 1000.80, 1001.50, 180000, '2032-10-14'),
('REL-8.25-31', '8.25% Reliance Industries Debenture 2031', 'Energy & Petrochem', 8.25, 1005.00, 0.12, 1004.50, 1005.50, 140000, '2033-10-14'),
('TATA-8.40-32', '8.40% Tata Steel Secured Debenture 2032', 'Metals & Mining', 8.40, 995.80, -0.05, 995.20, 996.20, 95000, '2034-10-14'),
('NHAI-7.90-27', '7.90% NHAI Tax-Free Infrastructure Bond 2027', 'National Highways', 7.90, 1014.50, 0.03, 1014.00, 1015.00, 320000, '2029-10-14')
ON CONFLICT (bond_symbol) DO NOTHING;

-- Seed 1,000 Additional Corporate Bonds & Debentures
DO $$
DECLARE
    i INT;
    v_sym VARCHAR(32);
    v_name VARCHAR(128);
    v_sec VARCHAR(64);
    v_coupon NUMERIC(6, 2);
    v_price NUMERIC(18, 4);
    v_chg NUMERIC(8, 4);
    v_bid NUMERIC(18, 4);
    v_ask NUMERIC(18, 4);
    v_vol BIGINT;
    v_mat DATE;
    v_issuers TEXT[] := ARRAY[
        'NABARD AAA Bond', 'SIDBI Secured Bond', 'EXIM Bank Infrastructure Bond', 'IRFC Railway Debenture', 'HUDCO Housing Bond',
        'NTPC Green Power Bond', 'IOCL Petrochemical Bond', 'BPCL Energy Debenture', 'GAIL Pipeline Bond', 'SAIL Steel Infra Bond',
        'LIC Housing Finance Bond', 'HDFC Credila Education Bond', 'Bajaj Finance AAA Debenture', 'L&T Finance Holdings Bond', 'Mahindra Finance Debenture',
        'Tata Motors Commercial Bond', 'ICICI Securities Primary Bond', 'Axis Finance Senior Bond', 'Kotak Prime Secured Debenture', 'SBI Cards Debenture',
        'Adani Ports Senior Secured', 'JSW Energy Clean Bond', 'ReNew Power Solar Bond', 'Torrent Power Grid Bond', 'Bharti Hexacom Network Bond',
        'Vedanta Aluminum Debenture', 'Grasim VSF Expansion Bond', 'Shree Cement Eco Bond', 'UltraTech Cement Debenture', 'Hindalco Copper Works Bond'
    ];
    v_prefixes TEXT[] := ARRAY[
        'NABARD', 'SIDBI', 'EXIM', 'IRFC', 'HUDCO',
        'NTPC', 'IOCL', 'BPCL', 'GAIL', 'SAIL',
        'LICHSG', 'HDFCCRED', 'BAJAJ', 'LTFIN', 'MMFIN',
        'TATAMOT', 'ICICIS', 'AXISFIN', 'KOTAKP', 'SBICARD',
        'ADANIPT', 'JSWENG', 'RENEW', 'TORRENT', 'BHARTIH',
        'VEDL', 'GRASIM', 'SHREE', 'ULTRATC', 'HINDALCO'
    ];
    v_sectors TEXT[] := ARRAY[
        'Financial Services', 'Financial Services', 'Financial Services', 'PSU / Infrastructure', 'PSU / Infrastructure',
        'Power & Utilities', 'Energy & Petrochem', 'Energy & Petrochem', 'Energy & Petrochem', 'Metals & Mining',
        'Financial Services', 'Financial Services', 'Financial Services', 'Financial Services', 'Financial Services',
        'Automotive & Transport', 'Financial Services', 'Financial Services', 'Financial Services', 'Financial Services',
        'PSU / Infrastructure', 'Power & Utilities', 'Power & Utilities', 'Power & Utilities', 'Telecommunications',
        'Metals & Mining', 'Metals & Mining', 'PSU / Infrastructure', 'PSU / Infrastructure', 'Metals & Mining'
    ];
    v_idx INT;
    v_series INT;
    v_year INT;
BEGIN
    FOR i IN 1..1000 LOOP
        v_idx := ((i - 1) % 30) + 1;
        v_series := ((i - 1) / 30) + 1;
        v_year := 2027 + (i % 22);
        
        v_coupon := ROUND((7.10 + ((i * 11) % 240) / 100.0)::numeric, 2);
        v_sym := v_prefixes[v_idx] || '-' || TO_CHAR(v_coupon, 'FM99.90') || '-' || SUBSTRING(v_year::text FROM 3 FOR 2) || '-' || LPAD(v_series::text, 2, '0');
        v_name := TO_CHAR(v_coupon, 'FM99.90') || '% ' || v_issuers[v_idx] || ' Series ' || v_series || ' ' || v_year;
        v_sec := v_sectors[v_idx];
        v_price := ROUND((980.00 + ((i * 19) % 450) / 10.0)::numeric, 2);
        v_chg := ROUND((((i * 7) % 40 - 20) / 100.0)::numeric, 2);
        v_bid := v_price - 0.50;
        v_ask := v_price + 0.50;
        v_vol := 25000 + ((i * 9800) % 750000);
        v_mat := (v_year::text || '-10-14')::date;
        
        INSERT INTO exchange.bonds (bond_symbol, issuer_series, sector_type, coupon_rate_pct, clean_price, change_pct, bid, ask, volume, maturity_date)
        VALUES (v_sym, v_name, v_sec, v_coupon, v_price, v_chg, v_bid, v_ask, v_vol, v_mat)
        ON CONFLICT (bond_symbol) DO NOTHING;
    END LOOP;
END $$;

-- Seed Canonical 7 Securities from Screenshot
INSERT INTO exchange.securities (security_symbol, security_description, category, benchmark_yield_pct, clean_price, change_pct, bid, ask, volume, maturity_date)
VALUES
('GS-7.18-2033', '7.18% Government of India Sovereign 2033', 'G-Sec', 7.18, 100.84, 0.04, 100.82, 100.86, 8500000, '2035-10-14'),
('GS-7.06-2028', '7.06% Government of India Sovereign 2028', 'G-Sec', 7.06, 100.50, 0.02, 100.48, 100.52, 4500000, '2030-10-14'),
('GS-7.26-2032', '7.26% Benchmark 10-Yr Sovereign 2032', 'G-Sec', 7.26, 101.15, 0.06, 101.12, 101.18, 9800000, '2034-10-14'),
('GS-7.38-2027', '7.38% Government of India Sovereign 2027', 'G-Sec', 7.38, 101.40, 0.01, 101.38, 101.42, 3800000, '2029-10-14'),
('TBILL-91D', '91-Day Sovereign Treasury Bill', 'T-Bill', 6.85, 98.32, 0.01, 98.31, 98.33, 12000000, '2026-11-28'),
('TBILL-182D', '182-Day Sovereign Treasury Bill', 'T-Bill', 6.95, 96.65, 0.02, 96.63, 96.67, 7500000, '2027-02-01'),
('TBILL-364D', '364-Day Sovereign Treasury Bill', 'T-Bill', 7.10, 93.25, 0.03, 93.22, 93.28, 6200000, '2027-06-11')
ON CONFLICT (security_symbol) DO NOTHING;

-- Seed 1,000 Additional Sovereign Securities & T-Bills
DO $$
DECLARE
    i INT;
    v_sym VARCHAR(32);
    v_desc VARCHAR(128);
    v_cat VARCHAR(64);
    v_yield NUMERIC(6, 2);
    v_price NUMERIC(18, 4);
    v_chg NUMERIC(8, 4);
    v_bid NUMERIC(18, 4);
    v_ask NUMERIC(18, 4);
    v_vol BIGINT;
    v_mat DATE;
    v_year INT;
    v_states TEXT[] := ARRAY[
        'Maharashtra', 'Gujarat', 'Tamil Nadu', 'Karnataka', 'Uttar Pradesh',
        'Telangana', 'Rajasthan', 'West Bengal', 'Madhya Pradesh', 'Kerala',
        'Andhra Pradesh', 'Punjab', 'Haryana', 'Odisha', 'Bihar',
        'Assam', 'Goa', 'Jharkhand', 'Chhattisgarh', 'Uttarakhand'
    ];
    v_state_codes TEXT[] := ARRAY[
        'MH', 'GJ', 'TN', 'KA', 'UP',
        'TG', 'RJ', 'WB', 'MP', 'KL',
        'AP', 'PB', 'HR', 'OD', 'BR',
        'AS', 'GA', 'JH', 'CG', 'UK'
    ];
    v_mod INT;
    v_st_idx INT;
BEGIN
    FOR i IN 1..1000 LOOP
        v_mod := i % 10;
        
        IF v_mod < 5 THEN
            -- Central G-Sec
            v_cat := 'G-Sec';
            v_year := 2028 + (i % 25);
            v_yield := ROUND((6.80 + ((i * 7) % 90) / 100.0)::numeric, 2);
            v_sym := 'GS-' || TO_CHAR(v_yield, 'FM99.90') || '-' || v_year || '-' || LPAD(i::text, 4, '0');
            v_desc := TO_CHAR(v_yield, 'FM99.90') || '% Government of India Sovereign ' || v_year || ' Tranche ' || i;
            v_price := ROUND((99.50 + ((i * 13) % 250) / 100.0)::numeric, 2);
            v_mat := (v_year::text || '-10-14')::date;
        ELSIF v_mod < 8 THEN
            -- State Development Loan (SDL)
            v_cat := 'G-Sec';
            v_st_idx := ((i - 1) % 20) + 1;
            v_year := 2029 + (i % 18);
            v_yield := ROUND((7.15 + ((i * 9) % 85) / 100.0)::numeric, 2);
            v_sym := 'SDL-' || v_state_codes[v_st_idx] || '-' || TO_CHAR(v_yield, 'FM99.90') || '-' || v_year || '-' || LPAD(i::text, 4, '0');
            v_desc := TO_CHAR(v_yield, 'FM99.90') || '% ' || v_states[v_st_idx] || ' State Development Loan ' || v_year;
            v_price := ROUND((99.80 + ((i * 11) % 220) / 100.0)::numeric, 2);
            v_mat := (v_year::text || '-08-20')::date;
        ELSE
            -- Treasury Bill (T-Bill)
            v_cat := 'T-Bill';
            v_year := 2026 + ((i % 4) + 1);
            v_yield := ROUND((6.65 + ((i * 5) % 65) / 100.0)::numeric, 2);
            IF i % 3 = 0 THEN
                v_sym := 'TBILL-91D-' || v_year || '-' || LPAD(i::text, 4, '0');
                v_desc := '91-Day Sovereign Treasury Bill Auction ' || v_year || ' #' || i;
                v_price := ROUND((98.10 + ((i * 3) % 40) / 100.0)::numeric, 2);
                v_mat := (v_year::text || '-03-15')::date;
            ELSIF i % 3 = 1 THEN
                v_sym := 'TBILL-182D-' || v_year || '-' || LPAD(i::text, 4, '0');
                v_desc := '182-Day Sovereign Treasury Bill Auction ' || v_year || ' #' || i;
                v_price := ROUND((96.40 + ((i * 4) % 50) / 100.0)::numeric, 2);
                v_mat := (v_year::text || '-06-25')::date;
            ELSE
                v_sym := 'TBILL-364D-' || v_year || '-' || LPAD(i::text, 4, '0');
                v_desc := '364-Day Sovereign Treasury Bill Auction ' || v_year || ' #' || i;
                v_price := ROUND((93.10 + ((i * 5) % 60) / 100.0)::numeric, 2);
                v_mat := (v_year::text || '-11-10')::date;
            END IF;
        END IF;
        
        v_chg := ROUND((((i * 3) % 15 - 5) / 100.0)::numeric, 2);
        v_bid := v_price - 0.02;
        v_ask := v_price + 0.02;
        v_vol := 1000000 + ((i * 85000) % 15000000);
        
        INSERT INTO exchange.securities (security_symbol, security_description, category, benchmark_yield_pct, clean_price, change_pct, bid, ask, volume, maturity_date)
        VALUES (v_sym, v_desc, v_cat, v_yield, v_price, v_chg, v_bid, v_ask, v_vol, v_mat)
        ON CONFLICT (security_symbol) DO NOTHING;
    END LOOP;
END $$;
