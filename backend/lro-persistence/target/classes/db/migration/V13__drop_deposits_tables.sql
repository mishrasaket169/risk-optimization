-- V13: Remove deposits table from all 4 bank schemas
-- As requested: Dropping legacy deposits table across bank_large, bank_medium, bank_small, bank_distressed

DROP TABLE IF EXISTS bank_large.deposits CASCADE;
DROP TABLE IF EXISTS bank_medium.deposits CASCADE;
DROP TABLE IF EXISTS bank_small.deposits CASCADE;
DROP TABLE IF EXISTS bank_distressed.deposits CASCADE;
