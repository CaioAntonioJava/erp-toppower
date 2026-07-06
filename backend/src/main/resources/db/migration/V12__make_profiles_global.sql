-- =============================================================================
-- V12__make_profiles_global.sql
--
-- Torna a tabela `profiles` GLOBAL (não tenant-scoped). O perfil representa
-- os dados pessoais do USUÁRIO (CPF, nome, telefone, e-mail de contato) —
-- pertence à pessoa, não à empresa (tenant) em que ela está operando no
-- momento. Um usuário vinculado a múltiplas empresas deve preencher o perfil
-- uma única vez; ao alternar entre tenants, o mesmo perfil continua visível,
-- sem duplicidade de CPF/dados.
--
-- Contexto:
--   O Profile foi introduzido como `extends BasePerson extends
--   TenantScopedEntity`, herdando portanto a coluna `tenant_uuid` e o filtro
--   Hibernate `tenantFilter`. Isso fazia com que `findByUserUuid` só
--   encontrasse o perfil dentro do tenant da sessão corrente — ao trocar de
--   tenant, o perfil "sumia" (404) e o frontend pedia "Complete seu perfil"
--   de novo, gerando duplicidade.
--
--   A entidade Java foi refatorada para `extends BaseEntity` (global), sem
--   `tenant_uuid`. Esta migration ajusta o schema persistido para refletir
--   essa decisão:
--     1) Consolidar perfis duplicados por user_id (defensivo — se já houve
--        duplicidade, manter apenas o mais antigo antes da unique constraint).
--     2) Adicionar unique constraint em user_id (garante a cardinalidade 1:1
--        que a entidade já declara via @JoinColumn(unique=true)).
--     3) Remover a coluna tenant_uuid e o índice idx_tenant de profiles.
--
-- Idempotente:
--   Roda em todo boot (spring.sql.init.mode=always). Cada bloco é guardado
--   por checagem em INFORMATION_SCHEMA. Em DB fresh (sem tenant_uuid em
--   profiles), apenas a unique constraint é aplicada (se ainda não existir).
--
-- Nota:
--   `ALTER TABLE ... DROP COLUMN IF EXISTS` não é suportado por todas as
--   versões do MySQL (apenas 8.0.29+); por isso os DROPs usam
--   PREPARE/EXECUTE dinâmico com guard em INFORMATION_SCHEMA, mesmo padrão
--   da V8.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. Consolidar perfis duplicados por user_id (manter o mais antigo).
--    Defensivo: se o bug de duplicidade já ocorreu em produção, removemos os
--    perfis extras antes de aplicar a unique constraint. Em base sem
--    duplicidade, o DELETE afeta zero linhas.
-- -----------------------------------------------------------------------------
DELETE p1 FROM profiles p1
INNER JOIN profiles p2
  ON p1.user_id = p2.user_id
  AND p1.uuid > p2.uuid;

-- -----------------------------------------------------------------------------
-- 2. Unique constraint em user_id (cardinalidade 1:1 User↔Profile).
--    Aplicada apenas se ainda não existir.
-- -----------------------------------------------------------------------------
SET @has_uk_user = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'profiles'
      AND NON_UNIQUE = 0
      AND COLUMN_NAME = 'user_id'
);
SET @sql = IF(@has_uk_user = 0,
    'ALTER TABLE profiles ADD UNIQUE KEY uk_profiles_user_id (user_id)',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- 3. Remover o índice idx_tenant de profiles (se existir).
-- -----------------------------------------------------------------------------
SET @has_idx_tenant = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'profiles'
      AND INDEX_NAME = 'idx_tenant'
);
SET @sql = IF(@has_idx_tenant = 1,
    'ALTER TABLE profiles DROP INDEX idx_tenant',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- 4. Remover a coluna tenant_uuid de profiles (se existir).
-- -----------------------------------------------------------------------------
SET @has_tenant_col = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'profiles'
      AND COLUMN_NAME = 'tenant_uuid'
);
SET @sql = IF(@has_tenant_col = 1,
    'ALTER TABLE profiles DROP COLUMN tenant_uuid',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;