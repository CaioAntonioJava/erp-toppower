-- Renomeia a coluna document_number para description na tabela boletos.
-- A coluna também aumenta o tamanho de VARCHAR(50) para VARCHAR(200) para
-- acomodar descrições mais longas.
ALTER TABLE boletos
    CHANGE COLUMN document_number description VARCHAR(200) NOT NULL;

-- Remove o índice antigo de document_number (será recriado com o novo nome).
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'boletos' AND INDEX_NAME = 'idx_boletos_document_number');
SET @sql = IF(@has_idx > 0, 'DROP INDEX idx_boletos_document_number ON boletos', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Recria o índice com o novo nome da coluna.
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'boletos' AND INDEX_NAME = 'idx_boletos_description');
SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_boletos_description ON boletos (description)', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Remove a unique key antiga (uk_boletos_org_document_number) e recria com o novo nome.
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'boletos' AND INDEX_NAME = 'uk_boletos_org_document_number');
SET @sql = IF(@has_idx > 0, 'ALTER TABLE boletos DROP INDEX uk_boletos_org_document_number', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'boletos' AND INDEX_NAME = 'uk_boletos_org_description');
SET @sql = IF(@has_idx = 0, 'CREATE UNIQUE INDEX uk_boletos_org_description ON boletos (organization_id, description)', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
