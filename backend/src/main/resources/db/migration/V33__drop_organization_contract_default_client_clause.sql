-- ============================================================================
-- Migration V33: Remove a coluna contract_default_client_clause
-- ============================================================================
-- A feature de "cláusula padrão do cliente contratante" com placeholders
-- (substituição automática de {CLIENT_NAME}, {CLIENT_DOCUMENT},
-- {CLIENT_ADDRESS}, etc.) foi descontinuada por não funcionar corretamente.
--
-- Esta migration:
--   1. Remove a coluna contract_default_client_clause da tabela organizations
--   2. Garante idempotência (no-op se a coluna não existir)
--
-- A coluna contract_default_description (V31) é mantida — ela é usada
-- independentemente como template da descrição de novos contratos.
-- ============================================================================

SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'organizations'
      AND COLUMN_NAME = 'contract_default_client_clause');
SET @sql = IF(@has_col = 1,
    'ALTER TABLE organizations DROP COLUMN contract_default_client_clause',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
