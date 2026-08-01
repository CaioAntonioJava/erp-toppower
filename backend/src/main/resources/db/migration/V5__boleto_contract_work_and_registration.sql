-- =============================================================================
-- V5__boleto_contract_work_and_registration.sql
--
-- Adiciona ao módulo de boletos:
--   1) Coluna contract_work_number (Nº Contrato/Obra, texto livre, opcional).
--   2) Coluna registration_date (data de cadastro informável, default = hoje).
--      Distinta do created_at (auditoria, preenchido automaticamente).
--   3) Índices auxiliares para as novas colunas.
--
-- Idempotente: spring.sql.init.mode=always roda a cada boot. As colunas
-- usam ADD COLUMN IF NOT EXISTS (MySQL 8+). Os índices seguem o padrão
-- do V1 (INFORMATION_SCHEMA + PREPARE), pois o MySQL não suporta
-- CREATE INDEX IF NOT EXISTS.
-- =============================================================================

-- 1) Coluna contract_work_number (texto livre, opcional)
SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'boletos'
      AND COLUMN_NAME = 'contract_work_number');
SET @sql = IF(@has_col = 0,
    'ALTER TABLE boletos ADD COLUMN contract_work_number VARCHAR(60) NULL',
    'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1b) Torna a coluna payee (beneficiário) nullable — campo agora opcional
SET @payee_nullable = (SELECT IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'boletos'
      AND COLUMN_NAME = 'payee');
SET @sql = IF(@payee_nullable = 'NO',
    'ALTER TABLE boletos MODIFY COLUMN payee VARCHAR(200) NULL',
    'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) Coluna registration_date (NOT NULL, default = data atual)
SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'boletos'
      AND COLUMN_NAME = 'registration_date');
SET @sql = IF(@has_col = 0,
    'ALTER TABLE boletos ADD COLUMN registration_date DATE NOT NULL DEFAULT (CURRENT_DATE)',
    'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Backfill: preenche registration_date de boletos existentes com a data
-- de criação (created_at), para que o NOT NULL não quebre registros antigos.
UPDATE boletos SET registration_date = DATE(created_at) WHERE registration_date IS NULL;

-- 3) Índice idx_boletos_contract_work sobre contract_work_number
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'boletos'
      AND INDEX_NAME = 'idx_boletos_contract_work');
SET @sql = IF(@has_idx = 0,
    'CREATE INDEX idx_boletos_contract_work ON boletos (contract_work_number)',
    'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4) Índice idx_boletos_registration_date sobre registration_date
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'boletos'
      AND INDEX_NAME = 'idx_boletos_registration_date');
SET @sql = IF(@has_idx = 0,
    'CREATE INDEX idx_boletos_registration_date ON boletos (registration_date)',
    'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;