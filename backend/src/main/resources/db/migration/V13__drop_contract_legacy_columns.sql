-- =============================================================================
-- V13__drop_contract_legacy_columns.sql
--
-- Remove colunas órfãs da tabela 'contracts' que não estão mapeadas na
-- entidade JPA Contract.java. Ambas são NOT NULL sem valor padrão, o que
-- causa falha em todos os INSERTs via Hibernate:
--   Field 'issue_date' doesn't have a default value
--   Field 'start_date' doesn't have a default value
--
-- - issue_date: coluna original de data de emissão; foi substituída por
--   validity_date na entidade, mas o rename da V11 não executou porque o
--   Hibernate já tinha criado validity_date antes (ddl-auto=update roda
--   antes das migrations), deixando issue_date órfã.
-- - start_date: coluna legada sem mapeamento na entidade.
--
-- Idempotente: verifica se cada coluna existe antes de dropar.
-- =============================================================================

-- Drop issue_date (substituída por validity_date)
SET @has_issue_date = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'contracts' AND COLUMN_NAME = 'issue_date');
SET @sql = IF(@has_issue_date > 0,
    'ALTER TABLE contracts DROP COLUMN issue_date',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Drop start_date (coluna legada sem mapeamento)
SET @has_start_date = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'contracts' AND COLUMN_NAME = 'start_date');
SET @sql = IF(@has_start_date > 0,
    'ALTER TABLE contracts DROP COLUMN start_date',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;