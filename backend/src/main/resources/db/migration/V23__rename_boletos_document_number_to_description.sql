-- Renomeia a coluna document_number para description na tabela boletos.
-- Totalmente idempotente: verifica a existência de cada coluna/índice antes
-- de alterar. O Hibernate (ddl-auto=update) roda antes desta migration e já
-- pode ter criado a coluna description a partir da entidade atualizada;
-- nesse caso, descartamos a coluna description vazia antes de renomear.

-- 1) Se a coluna description já existe (criada pelo Hibernate) e document_number
--    também existe, dropamos a description vazia para poder renomear.
SET @has_doc = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'boletos' AND COLUMN_NAME = 'document_number');
SET @has_desc = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'boletos' AND COLUMN_NAME = 'description');

-- 2) Se document_number existe, renomeia para description.
--    Antes, se description também existir, dropa description primeiro.
SET @sql = IF(@has_doc > 0 AND @has_desc > 0,
    'ALTER TABLE boletos DROP COLUMN description',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_doc = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'boletos' AND COLUMN_NAME = 'document_number');
SET @sql = IF(@has_doc > 0,
    'ALTER TABLE boletos CHANGE COLUMN document_number description VARCHAR(200) NOT NULL',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3) Remove o índice antigo de document_number (se ainda existir).
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'boletos' AND INDEX_NAME = 'idx_boletos_document_number');
SET @sql = IF(@has_idx > 0, 'DROP INDEX idx_boletos_document_number ON boletos', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4) Recria o índice com o novo nome da coluna (se não existir).
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'boletos' AND INDEX_NAME = 'idx_boletos_description');
SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_boletos_description ON boletos (description)', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 5) Remove a unique key antiga (uk_boletos_org_document_number) se existir.
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'boletos' AND INDEX_NAME = 'uk_boletos_org_document_number');
SET @sql = IF(@has_idx > 0, 'ALTER TABLE boletos DROP INDEX uk_boletos_org_document_number', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 6) Recria a unique key com o novo nome (se não existir).
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'boletos' AND INDEX_NAME = 'uk_boletos_org_description');
SET @sql = IF(@has_idx = 0, 'CREATE UNIQUE INDEX uk_boletos_org_description ON boletos (organization_id, description)', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
