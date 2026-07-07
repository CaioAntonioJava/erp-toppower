-- =============================================================================
-- V18__add_multi_organization.sql
--
-- Introduz o suporte Multiempresa (Organization) no ERP.
--
-- Arquitetura adotada: "Shared Database / Shared Schema" — uma única base
-- com isolamento lógico via coluna `organization_uuid` nas tabelas de negócio.
--
-- Ações (todas idempotentes, guardadas em INFORMATION_SCHEMA):
--   1) Cria a tabela `organizations` — cadastro de cada empresa (CNPJ próprio).
--   2) Cria a tabela `user_organizations` — relacionamento N:N usuário↔org,
--      com `role` por empresa e flag `is_default`.
--   3) Adiciona a coluna `organization_uuid` (BINARY(16), nullable) e o
--      índice `idx_organization` em cada tabela de negócio (14 tabelas,
--      mesma lista da V16__remove_multi_tenancy):
--        companies, customers, sellers, products, suppliers,
--        quotations, quotation_items,
--        sales_orders, sales_order_items,
--        stock_movements,
--        technical_proposals, technical_proposal_objectives,
--        technical_proposal_product_items, technical_proposal_service_items.
--
-- Coluna nullable é intencional:
--   * O `OrganizationEntityListener` garante preenchimento em novos inserts
--     (lendo o `OrganizationContext` da requisição).
--   * Rows legadas (em bases dev reconstruídas por ddl-auto=update) ficam
--     NULL e são excluídas das queries scoped pelo Hibernate `@Filter`.
--
-- Tabelas GLOBAIS (não recebem organization_uuid):
--   users, profiles, ceps, organizations, user_organizations.
--
-- Padrão PREPARE/EXECUTE dinâmico idêntico ao da V8/V12/V15/V16, pois
-- `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` nem sempre é suportado
-- (apenas MySQL 8.0.29+).
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. Tabela `organizations`
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS organizations (
    uuid                    BINARY(16)      NOT NULL,
    corporate_name          VARCHAR(200)    NOT NULL,
    trade_name              VARCHAR(200)    NOT NULL,
    cnpj                    VARCHAR(18)     NOT NULL,
    state_registration      VARCHAR(50)     NULL,
    municipal_registration VARCHAR(50)     NULL,
    phone                   VARCHAR(20)     NULL,
    email                   VARCHAR(100)   NULL,
    zip_code                VARCHAR(9)      NULL,
    street                  VARCHAR(200)   NULL,
    number                  VARCHAR(20)    NULL,
    district                VARCHAR(100)   NULL,
    city                    VARCHAR(100)   NULL,
    state                   VARCHAR(2)     NULL,
    complement              VARCHAR(100)   NULL,
    status                  VARCHAR(20)    NOT NULL DEFAULT 'ATIVO',
    created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
                                            ON UPDATE CURRENT_TIMESTAMP,
    created_by              VARCHAR(100)   NULL,
    updated_by              VARCHAR(100)   NULL,
    PRIMARY KEY (uuid),
    UNIQUE KEY uk_organizations_cnpj (cnpj)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- -----------------------------------------------------------------------------
-- 2. Tabela `user_organizations` (join N:N usuário↔organization)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_organizations (
    uuid                BINARY(16)      NOT NULL,
    user_uuid           BINARY(16)      NOT NULL,
    organization_uuid   BINARY(16)      NOT NULL,
    is_default          BOOLEAN         NOT NULL DEFAULT FALSE,
    role                VARCHAR(25)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
                                        ON UPDATE CURRENT_TIMESTAMP,
    created_by          VARCHAR(100)   NULL,
    updated_by          VARCHAR(100)   NULL,
    PRIMARY KEY (uuid),
    UNIQUE KEY uk_user_org (user_uuid, organization_uuid)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- -----------------------------------------------------------------------------
-- 3. Coluna organization_uuid + índice idx_organization nas tabelas de negócio.
--    Mesma lista da V16. Repetimos o bloco (add column + add index) por tabela.
-- -----------------------------------------------------------------------------

-- companies
SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'companies' AND COLUMN_NAME = 'organization_uuid');
SET @sql = IF(@has_col = 0, 'ALTER TABLE companies ADD COLUMN organization_uuid BINARY(16) NULL', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'companies' AND INDEX_NAME = 'idx_organization');
SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_organization ON companies (organization_uuid)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- customers
SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customers' AND COLUMN_NAME = 'organization_uuid');
SET @sql = IF(@has_col = 0, 'ALTER TABLE customers ADD COLUMN organization_uuid BINARY(16) NULL', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customers' AND INDEX_NAME = 'idx_organization');
SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_organization ON customers (organization_uuid)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sellers
SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sellers' AND COLUMN_NAME = 'organization_uuid');
SET @sql = IF(@has_col = 0, 'ALTER TABLE sellers ADD COLUMN organization_uuid BINARY(16) NULL', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sellers' AND INDEX_NAME = 'idx_organization');
SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_organization ON sellers (organization_uuid)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- products
SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'products' AND COLUMN_NAME = 'organization_uuid');
SET @sql = IF(@has_col = 0, 'ALTER TABLE products ADD COLUMN organization_uuid BINARY(16) NULL', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'products' AND INDEX_NAME = 'idx_organization');
SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_organization ON products (organization_uuid)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- suppliers
SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'suppliers' AND COLUMN_NAME = 'organization_uuid');
SET @sql = IF(@has_col = 0, 'ALTER TABLE suppliers ADD COLUMN organization_uuid BINARY(16) NULL', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'suppliers' AND INDEX_NAME = 'idx_organization');
SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_organization ON suppliers (organization_uuid)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- quotations
SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'quotations' AND COLUMN_NAME = 'organization_uuid');
SET @sql = IF(@has_col = 0, 'ALTER TABLE quotations ADD COLUMN organization_uuid BINARY(16) NULL', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'quotations' AND INDEX_NAME = 'idx_organization');
SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_organization ON quotations (organization_uuid)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- quotation_items
SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'quotation_items' AND COLUMN_NAME = 'organization_uuid');
SET @sql = IF(@has_col = 0, 'ALTER TABLE quotation_items ADD COLUMN organization_uuid BINARY(16) NULL', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'quotation_items' AND INDEX_NAME = 'idx_organization');
SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_organization ON quotation_items (organization_uuid)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sales_orders
SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sales_orders' AND COLUMN_NAME = 'organization_uuid');
SET @sql = IF(@has_col = 0, 'ALTER TABLE sales_orders ADD COLUMN organization_uuid BINARY(16) NULL', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sales_orders' AND INDEX_NAME = 'idx_organization');
SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_organization ON sales_orders (organization_uuid)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sales_order_items
SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sales_order_items' AND COLUMN_NAME = 'organization_uuid');
SET @sql = IF(@has_col = 0, 'ALTER TABLE sales_order_items ADD COLUMN organization_uuid BINARY(16) NULL', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sales_order_items' AND INDEX_NAME = 'idx_organization');
SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_organization ON sales_order_items (organization_uuid)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- stock_movements
SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'stock_movements' AND COLUMN_NAME = 'organization_uuid');
SET @sql = IF(@has_col = 0, 'ALTER TABLE stock_movements ADD COLUMN organization_uuid BINARY(16) NULL', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'stock_movements' AND INDEX_NAME = 'idx_organization');
SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_organization ON stock_movements (organization_uuid)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- technical_proposals
SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'technical_proposals' AND COLUMN_NAME = 'organization_uuid');
SET @sql = IF(@has_col = 0, 'ALTER TABLE technical_proposals ADD COLUMN organization_uuid BINARY(16) NULL', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'technical_proposals' AND INDEX_NAME = 'idx_organization');
SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_organization ON technical_proposals (organization_uuid)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- technical_proposal_objectives
SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'technical_proposal_objectives' AND COLUMN_NAME = 'organization_uuid');
SET @sql = IF(@has_col = 0, 'ALTER TABLE technical_proposal_objectives ADD COLUMN organization_uuid BINARY(16) NULL', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'technical_proposal_objectives' AND INDEX_NAME = 'idx_organization');
SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_organization ON technical_proposal_objectives (organization_uuid)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- technical_proposal_product_items
SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'technical_proposal_product_items' AND COLUMN_NAME = 'organization_uuid');
SET @sql = IF(@has_col = 0, 'ALTER TABLE technical_proposal_product_items ADD COLUMN organization_uuid BINARY(16) NULL', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'technical_proposal_product_items' AND INDEX_NAME = 'idx_organization');
SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_organization ON technical_proposal_product_items (organization_uuid)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- technical_proposal_service_items
SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'technical_proposal_service_items' AND COLUMN_NAME = 'organization_uuid');
SET @sql = IF(@has_col = 0, 'ALTER TABLE technical_proposal_service_items ADD COLUMN organization_uuid BINARY(16) NULL', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'technical_proposal_service_items' AND INDEX_NAME = 'idx_organization');
SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_organization ON technical_proposal_service_items (organization_uuid)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;