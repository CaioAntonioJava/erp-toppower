-- Migration: renomeia issue_date para validity_date (se a coluna antiga
-- ainda existir), adiciona colunas de referência a cliente (customer_id,
-- company_id) e atualiza índices.
-- Idempotente: usa IF NOT EXISTS / IF EXISTS / DROP INDEX IF EXISTS.
-- Nota: o Hibernate (ddl-auto=update) roda ANTES deste script e já pode
-- ter criado validity_date a partir da entidade atualizada. Por isso a
-- lógica de rename só executa se issue_date existir E validity_date não
-- existir.

-- 1) Renomeia issue_date para validity_date (apenas se a antiga existir
--    e a nova ainda não existir).
--    Nota: em bases atualizadas pela V10 revisada (sem issue_date) ou pela
--    V13 (que dropa issue_date), esta condição será falsa e o rename é
--    no-op — o que está correto, pois validity_date já existe ou issue_date
--    não existe mais.
SET @has_old = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'contracts' AND COLUMN_NAME = 'issue_date');
SET @has_new = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'contracts' AND COLUMN_NAME = 'validity_date');
SET @sql = IF(@has_old > 0 AND @has_new = 0,
    'ALTER TABLE contracts CHANGE COLUMN issue_date validity_date DATE NOT NULL',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) Adiciona customer_id (se não existir).
SET @has_customer = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'contracts' AND COLUMN_NAME = 'customer_id');
SET @sql = IF(@has_customer = 0,
    'ALTER TABLE contracts ADD COLUMN customer_id BIGINT NULL AFTER year',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3) Adiciona company_id (se não existir).
SET @has_company = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'contracts' AND COLUMN_NAME = 'company_id');
SET @sql = IF(@has_company = 0,
    'ALTER TABLE contracts ADD COLUMN company_id BIGINT NULL AFTER customer_id',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4) Remove o índice antigo idx_contract_issue_date (se existir) e cria
--    o novo idx_contract_validity_date (se não existir).
--    Nota: o índice idx_contract_issue_date pode não existir se a V10
--    já tiver sido atualizada para não criá-lo, ou se a V13 já o removeu
--    junto com a coluna issue_date.
SET @has_old_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'contracts' AND INDEX_NAME = 'idx_contract_issue_date');
SET @sql = IF(@has_old_idx > 0, 'DROP INDEX idx_contract_issue_date ON contracts', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_new_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'contracts' AND INDEX_NAME = 'idx_contract_validity_date');
SET @sql = IF(@has_new_idx = 0, 'CREATE INDEX idx_contract_validity_date ON contracts (validity_date)', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 5) Cria índices para customer_id e company_id (se não existirem).
SET @has_cust_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'contracts' AND INDEX_NAME = 'idx_contract_customer');
SET @sql = IF(@has_cust_idx = 0, 'CREATE INDEX idx_contract_customer ON contracts (customer_id)', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_comp_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'contracts' AND INDEX_NAME = 'idx_contract_company');
SET @sql = IF(@has_comp_idx = 0, 'CREATE INDEX idx_contract_company ON contracts (company_id)', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;