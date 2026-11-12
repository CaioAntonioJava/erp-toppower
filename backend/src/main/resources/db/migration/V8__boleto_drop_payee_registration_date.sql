-- =============================================================================
-- V8__boleto_drop_payee_registration_date.sql
--
-- Remove as colunas payee (beneficiário) e registration_date (data de
-- cadastro) da tabela boletos, conforme refatoração do módulo. O Hibernate
-- (ddl-auto=update) não dropa colunas automaticamente, então a remoção
-- precisa ser feita via migration explícita.
--
-- Idempotente: spring.sql.init.mode=always roda a cada boot. Os guards
-- via INFORMATION_SCHEMA evitam erros se as colunas/índices já não existirem.
-- =============================================================================

-- 1) Remoção do índice idx_boletos_registration_date (coluna será removida)
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'boletos'
      AND INDEX_NAME = 'idx_boletos_registration_date');
SET @sql = IF(@has_idx > 0,
    'DROP INDEX idx_boletos_registration_date ON boletos',
    'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) Remoção da coluna registration_date
SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'boletos'
      AND COLUMN_NAME = 'registration_date');
SET @sql = IF(@has_col > 0,
    'ALTER TABLE boletos DROP COLUMN registration_date',
    'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3) Remoção da coluna payee (beneficiário)
SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'boletos'
      AND COLUMN_NAME = 'payee');
SET @sql = IF(@has_col > 0,
    'ALTER TABLE boletos DROP COLUMN payee',
    'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;