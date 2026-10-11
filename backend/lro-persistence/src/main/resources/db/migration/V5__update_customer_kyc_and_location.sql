-- V5: Standardize customer KYC status to 'Verified' / 'Not Verified' and add 'location_category' column across all 4 bank schemas

-- 1. bank_large
ALTER TABLE bank_large.customers ADD COLUMN IF NOT EXISTS location_category VARCHAR(32) DEFAULT 'Tier 1';

UPDATE bank_large.customers SET kyc_status = 'Verified', location_category = 'Tier 1' WHERE customer_id = 'CUST-LRG-1001';
UPDATE bank_large.customers SET kyc_status = 'Verified', location_category = 'Tier 1' WHERE customer_id = 'CUST-LRG-1002';
UPDATE bank_large.customers SET kyc_status = 'Verified', location_category = 'Tier 2' WHERE customer_id = 'CUST-LRG-1003';
UPDATE bank_large.customers SET kyc_status = 'Not Verified', location_category = 'Tier 3' WHERE customer_id = 'CUST-LRG-1004';
UPDATE bank_large.customers SET kyc_status = 'Verified', location_category = 'Tier 1' WHERE customer_id = 'CUST-LRG-1005';

INSERT INTO bank_large.customers (customer_id, full_name, segment, kyc_status, location_category, risk_category, branch_name)
VALUES
('CUST-LRG-1006', 'Kisan Samriddhi Agro Producers', 'Retail', 'Not Verified', 'Rural', 'Low', 'Nashik Rural Branch')
ON CONFLICT (customer_id) DO UPDATE SET kyc_status = EXCLUDED.kyc_status, location_category = EXCLUDED.location_category;

ALTER TABLE bank_large.customers ALTER COLUMN location_category SET NOT NULL;
ALTER TABLE bank_large.customers ADD CONSTRAINT chk_lrg_kyc CHECK (kyc_status IN ('Verified', 'Not Verified'));
ALTER TABLE bank_large.customers ADD CONSTRAINT chk_lrg_loc CHECK (location_category IN ('Tier 1', 'Tier 2', 'Tier 3', 'Rural'));

-- 2. bank_medium
ALTER TABLE bank_medium.customers ADD COLUMN IF NOT EXISTS location_category VARCHAR(32) DEFAULT 'Tier 1';

INSERT INTO bank_medium.customers (customer_id, full_name, segment, kyc_status, location_category, risk_category, branch_name)
VALUES
('CUST-MED-1001', 'Amit Verma', 'Retail', 'Verified', 'Tier 2', 'Low', 'Pune Main Branch'),
('CUST-MED-1002', 'Sunita Kulkarni', 'Retail', 'Not Verified', 'Tier 2', 'Low', 'Shivaji Nagar Branch'),
('CUST-MED-1003', 'Sahyadri Auto Component Makers', 'SME', 'Verified', 'Tier 3', 'Medium', 'Pimpri MIDC Branch'),
('CUST-MED-1004', 'Vidarbha Cotton Ginning Corp', 'Corporate', 'Verified', 'Rural', 'Medium', 'Nagpur Commercial Branch')
ON CONFLICT (customer_id) DO UPDATE SET kyc_status = EXCLUDED.kyc_status, location_category = EXCLUDED.location_category;

ALTER TABLE bank_medium.customers ALTER COLUMN location_category SET NOT NULL;
ALTER TABLE bank_medium.customers ADD CONSTRAINT chk_med_kyc CHECK (kyc_status IN ('Verified', 'Not Verified'));
ALTER TABLE bank_medium.customers ADD CONSTRAINT chk_med_loc CHECK (location_category IN ('Tier 1', 'Tier 2', 'Tier 3', 'Rural'));

-- 3. bank_small
ALTER TABLE bank_small.customers ADD COLUMN IF NOT EXISTS location_category VARCHAR(32) DEFAULT 'Tier 1';

INSERT INTO bank_small.customers (customer_id, full_name, segment, kyc_status, location_category, risk_category, branch_name)
VALUES
('CUST-SML-1001', 'Santosh Jadhav', 'Retail', 'Verified', 'Tier 3', 'Low', 'Kolhapur Town Branch'),
('CUST-SML-1002', 'Rukmini Shinde', 'Retail', 'Not Verified', 'Rural', 'Low', 'Karad Rural Branch'),
('CUST-SML-1003', 'Mahalakshmi Dairy Co-op Society', 'SME', 'Verified', 'Rural', 'Medium', 'Sangli Branch'),
('CUST-SML-1004', 'Deccan Sugar Mills Syndicate', 'Corporate', 'Verified', 'Rural', 'Medium', 'Satara Agro Branch')
ON CONFLICT (customer_id) DO UPDATE SET kyc_status = EXCLUDED.kyc_status, location_category = EXCLUDED.location_category;

ALTER TABLE bank_small.customers ALTER COLUMN location_category SET NOT NULL;
ALTER TABLE bank_small.customers ADD CONSTRAINT chk_sml_kyc CHECK (kyc_status IN ('Verified', 'Not Verified'));
ALTER TABLE bank_small.customers ADD CONSTRAINT chk_sml_loc CHECK (location_category IN ('Tier 1', 'Tier 2', 'Tier 3', 'Rural'));

-- 4. bank_distressed
ALTER TABLE bank_distressed.customers ADD COLUMN IF NOT EXISTS location_category VARCHAR(32) DEFAULT 'Tier 1';

UPDATE bank_distressed.customers SET kyc_status = 'Verified', location_category = 'Tier 3' WHERE customer_id = 'CUST-DST-1001';
UPDATE bank_distressed.customers SET kyc_status = 'Not Verified', location_category = 'Tier 2' WHERE customer_id = 'CUST-DST-1002';

INSERT INTO bank_distressed.customers (customer_id, full_name, segment, kyc_status, location_category, risk_category, branch_name)
VALUES
('CUST-DST-1003', 'Dombivli Chemical Synthetics', 'SME', 'Not Verified', 'Tier 3', 'High', 'Dombivli MIDC Branch'),
('CUST-DST-1004', 'Raigad Minerals Extraction LLP', 'Corporate', 'Not Verified', 'Rural', 'High', 'Roha Industrial Branch')
ON CONFLICT (customer_id) DO UPDATE SET kyc_status = EXCLUDED.kyc_status, location_category = EXCLUDED.location_category;

ALTER TABLE bank_distressed.customers ALTER COLUMN location_category SET NOT NULL;
ALTER TABLE bank_distressed.customers ADD CONSTRAINT chk_dst_kyc CHECK (kyc_status IN ('Verified', 'Not Verified'));
ALTER TABLE bank_distressed.customers ADD CONSTRAINT chk_dst_loc CHECK (location_category IN ('Tier 1', 'Tier 2', 'Tier 3', 'Rural'));
