-- =============================================================================
-- V21__drop_organization_uuid_from_carriers.sql
--
-- Remove a coluna `organization_uuid` da tabela `carriers`, tornando a
-- entidade um dado global (compartilhado entre todas as empresas).
--
-- Motivo:
--   A entidade Carrier deixou de herdar de OrganizationScopedEntity e passou
--   a herdar de BaseEntity. Com isso, o filtro Hibernate "organizationFilter"
--   não se aplica mais a carriers, e a coluna `organization_uuid` ficou sem
--   uso — herdada da V20__add_organization_uuid_to_carriers.sql, que a
--   adicionou para suportar o isolamento que não existe mais.
--
--   Sintoma que motivou a mudança:
--     Gestores (ROLE_MANAGER) não viam transportadoras nos formulários de
--     venda. Além do @PreAuthorize restrito a ADMIN (corrigido no controller),
--     o filtro Hibernate WHERE organization_uuid = :organizationUuid excluía
--     as carriers cadastradas, cuja coluna ficava NULL (criadas sem contexto
--     de org ativa). Ao tornar Carrier global, o dropdown passa a listar as
--     transportadoras cadastradas para qualquer usuário autenticado.
--
-- Idempotente (PREPARE/EXECUTE dinâmico, mesmo padrão da V18/V20): roda
-- apenas se o objeto existir, evitando erro em re-deploy ou em bases onde
-- a V20 não chegou a rodar.
-- =============================================================================

-- Drop do índice de isolamento (idx_organization), se existir.
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'carriers'
      AND INDEX_NAME = 'idx_organization');
SET @sql = IF(@has_idx > 0,
    'ALTER TABLE carriers DROP INDEX idx_organization',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Drop da coluna organization_uuid, se existir.
SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'carriers'
      AND COLUMN_NAME = 'organization_uuid');
SET @sql = IF(@has_col > 0,
    'ALTER TABLE carriers DROP COLUMN organization_uuid',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;