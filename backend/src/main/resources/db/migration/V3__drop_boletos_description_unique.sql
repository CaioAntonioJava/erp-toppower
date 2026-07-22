-- =============================================================================
-- Remove a constraint de unicidade (organization_id, description) da tabela
-- boletos. Boletos podem ter a mesma descrição, inclusive dentro da mesma
-- organização.
-- =============================================================================

SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'boletos'
      AND INDEX_NAME = 'uk_boletos_org_description');
SET @sql = IF(@has_idx > 0,
    'DROP INDEX uk_boletos_org_description ON boletos',
    'DO 0');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
