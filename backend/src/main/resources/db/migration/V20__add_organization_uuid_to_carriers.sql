-- =============================================================================
-- V20__add_organization_uuid_to_carriers.sql
--
-- Adiciona a coluna `organization_uuid` à tabela `carriers`, esquecida na
-- V18__add_multi_organization.sql (que listava 14 tabelas de negócio mas
-- não incluía `carriers`, porque o módulo Carrier foi reintroduzido depois
-- pela V17 e a V18 foi escrita antes disso ser reconciliado).
--
-- Sintoma que motivou esta migration:
--   GET /api/v1/carriers retornava HTTP 403 Forbidden com corpo vazio.
--   Raiz: o Hibernate tentava `SELECT ... carriers.organization_uuid ...`
--   (porque a entidade Carrier herda OrganizationScopedEntity) e o MySQL
--   devolvia `Unknown column 'c1_0.organization_uuid' in 'field list'`.
--   A InvalidDataAccessResourceUsageException era capturada pelo
--   ExceptionTranslationFilter do Spring Security, que a traduzia em 403
--   AccessDenied antes de chegar ao GlobalExceptionHandler — daí o corpo
--   vazio e a falsa sensação de "sem permissão".
--
-- Por que o Hibernate `ddl-auto=update` não adicionou sozinho:
--   Em Spring Boot 4 / Hibernate 7, a estratégia `update` nem sempre cria
--   colunas novas em tabelas criadas por migration SQL (V17), especialmente
--   quando a tabela é recriada/dropada por migrations posteriores no mesmo
--   boot. Garantir via migration SQL idempotente é mais robusto e
--   independe da ordem Hibernate/SQL-init.
--
-- Idempotente (PREPARE/EXECUTE dinâmico, mesmo padrão da V18): roda
-- apenas se a coluna ainda não existir, evitando erro em re-deploy.
-- =============================================================================

SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'carriers'
      AND COLUMN_NAME = 'organization_uuid');
SET @sql = IF(@has_col = 0,
    'ALTER TABLE carriers ADD COLUMN organization_uuid BINARY(16) NULL',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Índice para acelerar o filtro de isolamento (WHERE organization_uuid = ?).
-- Mesmo nome usado pelas demais tabelas na V18 (idx_organization).
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'carriers'
      AND INDEX_NAME = 'idx_organization');
SET @sql = IF(@has_idx = 0,
    'CREATE INDEX idx_organization ON carriers (organization_uuid)',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;