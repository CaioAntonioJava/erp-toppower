-- =============================================================================
-- V7__boleto_refactor_fields.sql
--
-- Refatoração do módulo de boletos: novos campos de identificação do boleto
-- e remoção da coluna "description" (substituída pelos campos específicos).
--
-- Novos campos:
--   1) responsible_name  — nome do responsável (texto livre, opcional).
--   2) invoice_number    — número da nota fiscal (texto livre, opcional).
--   3) invoice_date      — data da nota fiscal (DATE, opcional).
--   4) installment_number — número da parcela (INT, opcional; manual ou auto).
--
-- Remoção:
--   5) DROP COLUMN description — não é mais usada pela entidade.
--
-- Idempotente: spring.sql.init.mode=always roda a cada boot. As colunas
-- usam ADD COLUMN IF NOT EXISTS via INFORMATION_SCHEMA + PREPARE. Os índices
-- seguem o padrão das migrations anteriores.
-- =============================================================================

-- 1) responsible_name (nome do responsável, opcional)
SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'boletos'
      AND COLUMN_NAME = 'responsible_name');
SET @sql = IF(@has_col = 0,
    'ALTER TABLE boletos ADD COLUMN responsible_name VARCHAR(120) NULL',
    'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) invoice_number (número da nota fiscal, opcional)
SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'boletos'
      AND COLUMN_NAME = 'invoice_number');
SET @sql = IF(@has_col = 0,
    'ALTER TABLE boletos ADD COLUMN invoice_number VARCHAR(60) NULL',
    'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3) invoice_date (data da nota fiscal, opcional)
SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'boletos'
      AND COLUMN_NAME = 'invoice_date');
SET @sql = IF(@has_col = 0,
    'ALTER TABLE boletos ADD COLUMN invoice_date DATE NULL',
    'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4) installment_number (número da parcela, opcional)
SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'boletos'
      AND COLUMN_NAME = 'installment_number');
SET @sql = IF(@has_col = 0,
    'ALTER TABLE boletos ADD COLUMN installment_number INT NULL',
    'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 5) Remoção da coluna description (não mais usada pela entidade)
SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'boletos'
      AND COLUMN_NAME = 'description');
SET @sql = IF(@has_col > 0,
    'ALTER TABLE boletos DROP COLUMN description',
    'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 6) Índice idx_boletos_responsible_name
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'boletos'
      AND INDEX_NAME = 'idx_boletos_responsible_name');
SET @sql = IF(@has_idx = 0,
    'CREATE INDEX idx_boletos_responsible_name ON boletos (responsible_name)',
    'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 7) Índice idx_boletos_invoice_number
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'boletos'
      AND INDEX_NAME = 'idx_boletos_invoice_number');
SET @sql = IF(@has_idx = 0,
    'CREATE INDEX idx_boletos_invoice_number ON boletos (invoice_number)',
    'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 8) Remoção do índice auxiliar idx_boletos_description (coluna foi removida)
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'boletos'
      AND INDEX_NAME = 'idx_boletos_description');
SET @sql = IF(@has_idx > 0,
    'DROP INDEX idx_boletos_description ON boletos',
    'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;