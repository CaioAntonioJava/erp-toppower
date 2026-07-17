-- =============================================================================
-- V13__fix_contract_start_date_nullable.sql
--
-- A coluna 'start_date' existe na tabela 'contracts' no banco de dados mas
-- não está mapeada na entidade JPA Contract.java. Como é NOT NULL sem valor
-- padrão, qualquer INSERT via Hibernate falha com:
--   Field 'start_date' doesn't have a default value
--
-- Esta migration torna a coluna NULL (nullable) para que o Hibernate possa
-- inserir registros sem precisar preenchê-la. A coluna permanece no banco
-- para não quebrar queries legadas, mas não é mais utilizada pela aplicação.
--
-- Idempotente: verifica se a coluna existe antes de alterar.
-- =============================================================================

SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'contracts' AND COLUMN_NAME = 'start_date');
SET @has_not_null = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'contracts' AND COLUMN_NAME = 'start_date'
    AND IS_NULLABLE = 'NO');
SET @sql = IF(@has_col > 0 AND @has_not_null > 0,
    'ALTER TABLE contracts MODIFY COLUMN start_date DATE NULL',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;