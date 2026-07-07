-- =============================================================================
-- V15__drop_carriers.sql
--
-- Remove o schema legado do módulo Carrier (transportadora), que foi
-- excluído do projeto. O Hibernate `ddl-auto=update` NÃO dropa tabelas
-- nem colunas órfãs, por isso esta migration é necessária para limpar
-- bancos já populados por boots anteriores.
--
-- Ações idempotentes (guardadas em INFORMATION_SCHEMA):
--   1) DROP da coluna `carrier_uuid` de `quotations`.
--   2) DROP da coluna `carrier_uuid` de `sales_orders`.
--   3) DROP da tabela `carriers` (e seus índices — uk_carrier_name,
--      uk_carrier_name_tenant, idx_tenant — caem junto).
--
-- Em DB fresh (sem Carrier nunca criado), todos os blocos são no-op.
--
-- Padrão de PREPARE/EXECUTE dinâmico igual ao da V8 e V12, já que
-- `ALTER TABLE ... DROP COLUMN IF EXISTS` nem sempre é suportado
-- (apenas MySQL 8.0.29+).
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. Remover a coluna carrier_uuid de quotations (se existir).
-- -----------------------------------------------------------------------------
SET @has_quotation_carrier = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'quotations'
      AND COLUMN_NAME = 'carrier_uuid'
);
SET @sql = IF(@has_quotation_carrier = 1,
    'ALTER TABLE quotations DROP COLUMN carrier_uuid',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- 2. Remover a coluna carrier_uuid de sales_orders (se existir).
-- -----------------------------------------------------------------------------
SET @has_sales_order_carrier = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sales_orders'
      AND COLUMN_NAME = 'carrier_uuid'
);
SET @sql = IF(@has_sales_order_carrier = 1,
    'ALTER TABLE sales_orders DROP COLUMN carrier_uuid',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- 3. Remover a tabela carriers (se existir).
--    Os índices/unique constraints (uk_carrier_name, uk_carrier_name_tenant,
--    idx_tenant) são dropados automaticamente com a tabela.
-- -----------------------------------------------------------------------------
SET @has_carriers_table = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'carriers'
);
SET @sql = IF(@has_carriers_table = 1,
    'DROP TABLE carriers',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;