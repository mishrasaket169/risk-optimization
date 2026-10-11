-- V8: Standardize customer_id and account_id format to 5 digits (XXXXX) starting from 00001
-- bank_large:
--   customer_id: CUST-LRG-00001 .. CUST-LRG-10000
--   account_id:  ACC-LRG-00001  .. ACC-LRG-25000
-- bank_medium:
--   customer_id: CUST-MED-00001 .. CUST-MED-07500
--   account_id:  ACC-MED-00001  .. ACC-MED-15000
-- bank_small:
--   customer_id: CUST-SML-00001 .. CUST-SML-05000
--   account_id:  ACC-SML-00001  .. ACC-SML-12000
-- bank_distressed:
--   customer_id: CUST-DST-00001 .. CUST-DST-04000
--   account_id:  ACC-DST-00001  .. ACC-DST-08000

-- =========================================================================
-- 1. Drop foreign keys across all bank schemas temporarily for atomic update
-- =========================================================================
ALTER TABLE bank_large.accounts DROP CONSTRAINT IF EXISTS accounts_customer_id_fkey;
ALTER TABLE bank_large.deposits DROP CONSTRAINT IF EXISTS deposits_customer_id_fkey;
ALTER TABLE bank_large.loans DROP CONSTRAINT IF EXISTS loans_customer_id_fkey;

ALTER TABLE bank_medium.accounts DROP CONSTRAINT IF EXISTS accounts_customer_id_fkey;
ALTER TABLE bank_small.accounts DROP CONSTRAINT IF EXISTS accounts_customer_id_fkey;
ALTER TABLE bank_distressed.accounts DROP CONSTRAINT IF EXISTS accounts_customer_id_fkey;
ALTER TABLE bank_distressed.loans DROP CONSTRAINT IF EXISTS loans_customer_id_fkey;

-- =========================================================================
-- 2. bank_large (10,000 customers, 25,000 accounts)
-- =========================================================================
UPDATE bank_large.customers 
SET customer_id = 'CUST-LRG-' || lpad((substring(customer_id from 10)::int - 1000)::text, 5, '0');

UPDATE bank_large.accounts 
SET 
    account_id = 'ACC-LRG-' || lpad((substring(account_id from 9)::int - 5000)::text, 5, '0'),
    customer_id = 'CUST-LRG-' || lpad((substring(customer_id from 10)::int - 1000)::text, 5, '0');

UPDATE bank_large.deposits 
SET customer_id = 'CUST-LRG-' || lpad((substring(customer_id from 10)::int - 1000)::text, 5, '0');

UPDATE bank_large.loans 
SET customer_id = 'CUST-LRG-' || lpad((substring(customer_id from 10)::int - 1000)::text, 5, '0');

-- =========================================================================
-- 3. bank_medium (7,500 customers, 15,000 accounts)
-- =========================================================================
UPDATE bank_medium.customers 
SET customer_id = 'CUST-MED-' || lpad((substring(customer_id from 10)::int - 1000)::text, 5, '0');

UPDATE bank_medium.accounts 
SET 
    account_id = 'ACC-MED-' || lpad((substring(account_id from 9)::int - 5000)::text, 5, '0'),
    customer_id = 'CUST-MED-' || lpad((substring(customer_id from 10)::int - 1000)::text, 5, '0');

-- =========================================================================
-- 4. bank_small (5,000 customers, 12,000 accounts)
-- =========================================================================
UPDATE bank_small.customers 
SET customer_id = 'CUST-SML-' || lpad((substring(customer_id from 10)::int - 1000)::text, 5, '0');

UPDATE bank_small.accounts 
SET 
    account_id = 'ACC-SML-' || lpad((substring(account_id from 9)::int - 5000)::text, 5, '0'),
    customer_id = 'CUST-SML-' || lpad((substring(customer_id from 10)::int - 1000)::text, 5, '0');

-- =========================================================================
-- 5. bank_distressed (4,000 customers, 8,000 accounts)
-- =========================================================================
UPDATE bank_distressed.customers 
SET customer_id = 'CUST-DST-' || lpad((substring(customer_id from 10)::int - 1000)::text, 5, '0');

UPDATE bank_distressed.accounts 
SET 
    account_id = 'ACC-DST-' || lpad((substring(account_id from 9)::int - 5000)::text, 5, '0'),
    customer_id = 'CUST-DST-' || lpad((substring(customer_id from 10)::int - 1000)::text, 5, '0');

UPDATE bank_distressed.loans 
SET customer_id = 'CUST-DST-' || lpad((substring(customer_id from 10)::int - 1000)::text, 5, '0');

-- =========================================================================
-- 6. Re-establish foreign keys with referential integrity
-- =========================================================================
ALTER TABLE bank_large.accounts ADD CONSTRAINT accounts_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES bank_large.customers(customer_id);
ALTER TABLE bank_large.deposits ADD CONSTRAINT deposits_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES bank_large.customers(customer_id);
ALTER TABLE bank_large.loans ADD CONSTRAINT loans_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES bank_large.customers(customer_id);

ALTER TABLE bank_medium.accounts ADD CONSTRAINT accounts_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES bank_medium.customers(customer_id);
ALTER TABLE bank_small.accounts ADD CONSTRAINT accounts_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES bank_small.customers(customer_id);
ALTER TABLE bank_distressed.accounts ADD CONSTRAINT accounts_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES bank_distressed.customers(customer_id);
