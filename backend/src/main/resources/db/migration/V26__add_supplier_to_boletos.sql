-- Adiciona a coluna supplier_id à tabela boletos, permitindo vincular
-- um boleto a um fornecedor cadastrado. Quando supplier_id está presente,
-- o BoletoService dispara a geração automática de uma conta a pagar.
-- Idempotente: roda a cada boot, por isso o guard via INFORMATION_SCHEMA.
SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'boletos' AND COLUMN_NAME = 'supplier_id');
SET @sql = IF(@has_col = 0,
    'ALTER TABLE boletos ADD COLUMN supplier_id BIGINT NULL',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Índice auxiliar para consultas de boletos por fornecedor (idempotente).
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'boletos' AND INDEX_NAME = 'idx_boletos_supplier_id');
SET @sql = IF(@has_idx = 0,
    'CREATE INDEX idx_boletos_supplier_id ON boletos (supplier_id)',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;