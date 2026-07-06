-- =============================================================================
-- V13__dedup_and_uk_carrier_name_tenant.sql
--
-- Garante unicidade de (carrier_name, tenant_uuid) na tabela `carriers`.
--
-- Contexto:
--   A entidade Carrier não declarava @UniqueConstraint, e o único guard
--   contra duplicatas era o check aplicacional `existsByCarrierNameAndTenant
--   uuid` no CarrierSeedRunner — não à prova de concorrência entre boots.
--   Como a migration V10 (que purgeava carriers a cada boot) foi removida
--   do schema-locations, duplicatas históricas passaram a persistir e a
--   acumular a cada boot do seed runner, aparecendo duplicadas no dropdown
--   do frontend.
--
--   Esta migration:
--     1) Consolida duplicatas por (carrier_name, tenant_uuid), mantendo o
--        registro mais antigo (menor uuid) de cada par. Referências lógicas
--        em quotations/sales_orders apontam para carrier_uuid; registros
--        órfãos removidos não estão referenciados (carriers seed têm
--        freight_value opcional e não são pai em FK física).
--     2) Adiciona a unique constraint `uk_carrier_name_tenant`, espelhando o
--        @UniqueConstraint declarado na entidade Carrier. Sem este passo,
--        o Hibernate `ddl-auto=update` não cria a constraint se já houver
--        duplicatas, deixando o bug latente.
--
-- Idempotente:
--   Roda em todo boot (spring.sql.init.mode=always). O DELETE só remove
--   duplicatas; em base já consolidada afeta zero linhas. A unique
--   constraint só é adicionada se ainda não existir (guard em
--   INFORMATION_SCHEMA). Em DB fresh, a constraint é criada no primeiro
--   boot.
--
-- Nota:
--   A coluna `carrier_name` admite NULL (transportadora placeholder), por
--   isso a constraint deve permitir múltiplos NULLs por tenant — MySQL
--   trata NULL como distinto em índices UNIQUE por padrão, então isso é
--   seguro.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. Consolidar duplicatas por (carrier_name, tenant_uuid).
--    Mantém o registro com o menor uuid (mais antigo) de cada par.
--    Pares com carrier_name NULL não são considerados duplicados (NULL !=
--    NULL em MySQL), então placeholders não são removidos.
-- -----------------------------------------------------------------------------
DELETE c1 FROM carriers c1
INNER JOIN carriers c2
  ON c1.carrier_name = c2.carrier_name
  AND c1.carrier_name IS NOT NULL
  AND c1.tenant_uuid = c2.tenant_uuid
  AND c1.uuid > c2.uuid;

-- -----------------------------------------------------------------------------
-- 2. Unique constraint uk_carrier_name_tenant em (carrier_name, tenant_uuid).
--    Aplicada apenas se ainda não existir.
-- -----------------------------------------------------------------------------
SET @has_uk_carrier_name_tenant = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'carriers'
      AND INDEX_NAME = 'uk_carrier_name_tenant'
);
SET @sql = IF(@has_uk_carrier_name_tenant = 0,
    'ALTER TABLE carriers ADD UNIQUE KEY uk_carrier_name_tenant (carrier_name, tenant_uuid)',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;