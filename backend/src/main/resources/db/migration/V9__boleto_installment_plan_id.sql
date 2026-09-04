-- =============================================================================
-- V9__boleto_installment_plan_id.sql
--
-- Adiciona a coluna installment_plan_id na tabela boletos para agrupar
-- as parcelas geradas em um mesmo plano de parcelamento (UUID string).
-- Permite recuperar, listar ou cancelar todas as parcelas de um mesmo
-- plano posteriormente. Nula para boletos avulsos (sem parcelamento).
--
-- Idempotente: spring.sql.init.mode=always roda a cada boot. Os guards
-- via INFORMATION_SCHEMA evitam erros se a coluna/índice já existirem.
-- =============================================================================

-- 1) Adição da coluna installment_plan_id
SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'boletos'
      AND COLUMN_NAME = 'installment_plan_id');
SET @sql = IF(@has_col = 0,
    'ALTER TABLE boletos ADD COLUMN installment_plan_id VARCHAR(36) NULL',
    'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) Índice para buscar parcelas do mesmo plano
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'boletos'
      AND INDEX_NAME = 'idx_boletos_installment_plan_id');
SET @sql = IF(@has_idx = 0,
    'CREATE INDEX idx_boletos_installment_plan_id ON boletos(installment_plan_id)',
    'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;