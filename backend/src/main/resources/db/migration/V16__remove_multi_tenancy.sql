-- =============================================================================
-- V16__remove_multi_tenancy.sql
--
-- Remove todo o schema de multi-tenancy do projeto. O sistema passa a ser
-- single-tenant: não há mais tabela `tenants`, `user_tenants` nem coluna
-- `tenant_uuid` nas entidades de negócio.
--
-- O Hibernate `ddl-auto=update` NÃO dropa tabelas nem colunas órfãs, por isso
-- esta migration é necessária para limpar bancos já populados por boots
-- anteriores (V11 criava o schema de multi-tenancy; V12 já removia o
-- tenant_uuid de `profiles`).
--
-- Ações idempotentes (guardadas em INFORMATION_SCHEMA):
--   1) DROP das tabelas `user_tenants` e `tenants` (nesta ordem — a primeira
--      referencia logicamente a segunda).
--   2) DROP do índice `idx_tenant` e da coluna `tenant_uuid` de cada tabela
--      de negócio (14 tabelas):
--        companies, customers, sellers, products, suppliers,
--        quotations, quotation_items,
--        sales_orders, sales_order_items,
--        stock_movements,
--        technical_proposals, technical_proposal_objectives,
--        technical_proposal_product_items, technical_proposal_service_items.
--   3) DROP do índice `idx_tenant` e da coluna `tenant_uuid` de `profiles`
--      (caso a V12 não tenha sido aplicada em algum banco legado).
--
-- Em DB fresh (sem multi-tenancy nunca instalado), todos os blocos são no-op.
--
-- Padrão de PREPARE/EXECUTE dinâmico igual ao da V8, V12 e V15, já que
-- `ALTER TABLE ... DROP COLUMN IF EXISTS` nem sempre é suportado
-- (apenas MySQL 8.0.29+).
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. DROP das tabelas user_tenants e tenants (se existirem).
-- -----------------------------------------------------------------------------

SET @has_user_tenants = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_tenants'
);
SET @sql = IF(@has_user_tenants = 1, 'DROP TABLE user_tenants', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_tenants = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tenants'
);
SET @sql = IF(@has_tenants = 1, 'DROP TABLE tenants', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- 2. DROP do índice idx_tenant e da coluna tenant_uuid das tabelas de negócio.
--    As tabelas são iteradas uma a uma com guards em INFORMATION_SCHEMA.
-- -----------------------------------------------------------------------------

-- Lista de tabelas de negócio que tinham tenant_uuid:
--   companies, customers, sellers, products, suppliers,
--   quotations, quotation_items,
--   sales_orders, sales_order_items,
--   stock_movements,
--   technical_proposals, technical_proposal_objectives,
--   technical_proposal_product_items, technical_proposal_service_items,
--   profiles (defensivo — V12 já deveria ter removido).
--
-- MySQL não tem FOREACH em SQL puro, então repetimos o bloco
-- (drop idx_tenant + drop tenant_uuid) por tabela, de forma explícita.

-- companies
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'companies' AND INDEX_NAME = 'idx_tenant');
SET @sql = IF(@has_idx = 1, 'ALTER TABLE companies DROP INDEX idx_tenant', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'companies' AND COLUMN_NAME = 'tenant_uuid');
SET @sql = IF(@has_col = 1, 'ALTER TABLE companies DROP COLUMN tenant_uuid', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- customers
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customers' AND INDEX_NAME = 'idx_tenant');
SET @sql = IF(@has_idx = 1, 'ALTER TABLE customers DROP INDEX idx_tenant', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customers' AND COLUMN_NAME = 'tenant_uuid');
SET @sql = IF(@has_col = 1, 'ALTER TABLE customers DROP COLUMN tenant_uuid', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sellers
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sellers' AND INDEX_NAME = 'idx_tenant');
SET @sql = IF(@has_idx = 1, 'ALTER TABLE sellers DROP INDEX idx_tenant', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sellers' AND COLUMN_NAME = 'tenant_uuid');
SET @sql = IF(@has_col = 1, 'ALTER TABLE sellers DROP COLUMN tenant_uuid', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- products
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'products' AND INDEX_NAME = 'idx_tenant');
SET @sql = IF(@has_idx = 1, 'ALTER TABLE products DROP INDEX idx_tenant', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'products' AND COLUMN_NAME = 'tenant_uuid');
SET @sql = IF(@has_col = 1, 'ALTER TABLE products DROP COLUMN tenant_uuid', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- suppliers
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'suppliers' AND INDEX_NAME = 'idx_tenant');
SET @sql = IF(@has_idx = 1, 'ALTER TABLE suppliers DROP INDEX idx_tenant', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'suppliers' AND COLUMN_NAME = 'tenant_uuid');
SET @sql = IF(@has_col = 1, 'ALTER TABLE suppliers DROP COLUMN tenant_uuid', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- quotations
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'quotations' AND INDEX_NAME = 'idx_tenant');
SET @sql = IF(@has_idx = 1, 'ALTER TABLE quotations DROP INDEX idx_tenant', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'quotations' AND COLUMN_NAME = 'tenant_uuid');
SET @sql = IF(@has_col = 1, 'ALTER TABLE quotations DROP COLUMN tenant_uuid', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- quotation_items
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'quotation_items' AND INDEX_NAME = 'idx_tenant');
SET @sql = IF(@has_idx = 1, 'ALTER TABLE quotation_items DROP INDEX idx_tenant', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'quotation_items' AND COLUMN_NAME = 'tenant_uuid');
SET @sql = IF(@has_col = 1, 'ALTER TABLE quotation_items DROP COLUMN tenant_uuid', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sales_orders
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sales_orders' AND INDEX_NAME = 'idx_tenant');
SET @sql = IF(@has_idx = 1, 'ALTER TABLE sales_orders DROP INDEX idx_tenant', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sales_orders' AND COLUMN_NAME = 'tenant_uuid');
SET @sql = IF(@has_col = 1, 'ALTER TABLE sales_orders DROP COLUMN tenant_uuid', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sales_order_items
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sales_order_items' AND INDEX_NAME = 'idx_tenant');
SET @sql = IF(@has_idx = 1, 'ALTER TABLE sales_order_items DROP INDEX idx_tenant', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sales_order_items' AND COLUMN_NAME = 'tenant_uuid');
SET @sql = IF(@has_col = 1, 'ALTER TABLE sales_order_items DROP COLUMN tenant_uuid', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- stock_movements
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'stock_movements' AND INDEX_NAME = 'idx_tenant');
SET @sql = IF(@has_idx = 1, 'ALTER TABLE stock_movements DROP INDEX idx_tenant', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'stock_movements' AND COLUMN_NAME = 'tenant_uuid');
SET @sql = IF(@has_col = 1, 'ALTER TABLE stock_movements DROP COLUMN tenant_uuid', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- technical_proposals
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'technical_proposals' AND INDEX_NAME = 'idx_tenant');
SET @sql = IF(@has_idx = 1, 'ALTER TABLE technical_proposals DROP INDEX idx_tenant', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'technical_proposals' AND COLUMN_NAME = 'tenant_uuid');
SET @sql = IF(@has_col = 1, 'ALTER TABLE technical_proposals DROP COLUMN tenant_uuid', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- technical_proposal_objectives
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'technical_proposal_objectives' AND INDEX_NAME = 'idx_tenant');
SET @sql = IF(@has_idx = 1, 'ALTER TABLE technical_proposal_objectives DROP INDEX idx_tenant', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'technical_proposal_objectives' AND COLUMN_NAME = 'tenant_uuid');
SET @sql = IF(@has_col = 1, 'ALTER TABLE technical_proposal_objectives DROP COLUMN tenant_uuid', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- technical_proposal_product_items
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'technical_proposal_product_items' AND INDEX_NAME = 'idx_tenant');
SET @sql = IF(@has_idx = 1, 'ALTER TABLE technical_proposal_product_items DROP INDEX idx_tenant', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'technical_proposal_product_items' AND COLUMN_NAME = 'tenant_uuid');
SET @sql = IF(@has_col = 1, 'ALTER TABLE technical_proposal_product_items DROP COLUMN tenant_uuid', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- technical_proposal_service_items
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'technical_proposal_service_items' AND INDEX_NAME = 'idx_tenant');
SET @sql = IF(@has_idx = 1, 'ALTER TABLE technical_proposal_service_items DROP INDEX idx_tenant', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'technical_proposal_service_items' AND COLUMN_NAME = 'tenant_uuid');
SET @sql = IF(@has_col = 1, 'ALTER TABLE technical_proposal_service_items DROP COLUMN tenant_uuid', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- profiles (defensivo — V12 já deveria ter removido, mas garante em bancos legados)
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'profiles' AND INDEX_NAME = 'idx_tenant');
SET @sql = IF(@has_idx = 1, 'ALTER TABLE profiles DROP INDEX idx_tenant', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'profiles' AND COLUMN_NAME = 'tenant_uuid');
SET @sql = IF(@has_col = 1, 'ALTER TABLE profiles DROP COLUMN tenant_uuid', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;