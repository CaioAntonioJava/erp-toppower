-- Remove a coluna document_number da tabela accounts_payable, que é um
-- resíduo de versão anterior do schema. A entidade JPA Payable.java não
-- possui este campo, causando erro ao inserir registros.
-- Totalmente idempotente.
SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'accounts_payable' AND COLUMN_NAME = 'document_number');
SET @sql = IF(@has_col > 0, 'ALTER TABLE accounts_payable DROP COLUMN document_number', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
