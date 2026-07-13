-- ============================================================================
-- Migration: V39__make_zip_code_nullable
-- Descrição: Torna o campo CEP (zip_code) opcional em todas as tabelas que
--            o possuem como obrigatório. O CEP passa a ser não obrigatório
--            tanto no backend quanto no frontend.
-- ============================================================================

-- companies.address_zip_code
ALTER TABLE companies MODIFY COLUMN address_zip_code VARCHAR(9) NULL;

-- customers.address_zip_code
ALTER TABLE customers MODIFY COLUMN address_zip_code VARCHAR(9) NULL;

-- suppliers.address_zip_code
ALTER TABLE suppliers MODIFY COLUMN address_zip_code VARCHAR(9) NULL;

-- contracts.address_zip_code
ALTER TABLE contracts MODIFY COLUMN address_zip_code VARCHAR(9) NULL;

-- technical_proposals.address_zip_code
ALTER TABLE technical_proposals MODIFY COLUMN address_zip_code VARCHAR(9) NULL;
