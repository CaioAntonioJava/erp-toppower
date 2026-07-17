-- =============================================================================
-- V14__drop_contract_clause_description.sql
--
-- A tabela 'contract_clauses' pode conter uma coluna 'description' órfã
-- (NOT NULL sem default) que não está mapeada na entidade JPA
-- ContractClause.java — a entidade usa 'content', não 'description'.
-- Isso causa falha em todos os INSERTs:
--   Field 'description' doesn't have a default value
--
-- Idempotente: verifica se a coluna existe antes de dropar.
-- =============================================================================

SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'contract_clauses' AND COLUMN_NAME = 'description');
SET @sql = IF(@has_col > 0,
    'ALTER TABLE contract_clauses DROP COLUMN description',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;