-- =============================================================================
-- V1__init.sql
--
-- Migration única e consolidada para o ERP TopPower.
--
-- O Hibernate (ddl-auto=update) já cria todas as tabelas e colunas a partir
-- das entidades JPA. Este script corrige/adiciona o que o Hibernate não
-- gera automaticamente:
--
--   1) Índices idx_organization nas tabelas organization-scoped
--      (o Hibernate não cria índices em colunas FK-like sem @Index explícito).
--   2) Unique constraints compostas (organization_id + coluna) que as
--      entidades declaram com nome de coluna legado "organization_uuid"
--      (resíduo da era UUID) — o Hibernate criaria constraints quebradas.
--   3) Seed das duas Organizations Top Power (idempotente).
--
-- Idempotente: todos os blocos usam INFORMATION_SCHEMA para verificar
-- existência antes de criar/dropar.
-- =============================================================================

-- =============================================================================
-- 1. Índices idx_organization nas tabelas organization-scoped
-- =============================================================================
-- Lista de tabelas que herdam de OrganizationScopedEntity e precisam
-- de índice em organization_id para performance do filtro Hibernate.

SET @tables_org_scoped = 'companies,customers,sellers,products,suppliers,quotations,quotation_items,sales_orders,sales_order_items,stock_movements,technical_proposals,technical_proposal_product_items,technical_proposal_service_items,contracts,contract_clauses,contract_product_items,contract_service_items';

-- Como MySQL não tem FOREACH, geramos dinamicamente os comandos.
-- Para cada tabela na lista, verificamos se o índice idx_organization existe
-- e criamos se necessário.

-- companies
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'companies' AND INDEX_NAME = 'idx_organization');
SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_organization ON companies (organization_id)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- customers
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customers' AND INDEX_NAME = 'idx_organization');
SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_organization ON customers (organization_id)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sellers
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sellers' AND INDEX_NAME = 'idx_organization');
SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_organization ON sellers (organization_id)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- products
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'products' AND INDEX_NAME = 'idx_organization');
SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_organization ON products (organization_id)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- suppliers
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'suppliers' AND INDEX_NAME = 'idx_organization');
SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_organization ON suppliers (organization_id)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- quotations
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'quotations' AND INDEX_NAME = 'idx_organization');
SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_organization ON quotations (organization_id)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- quotation_items
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'quotation_items' AND INDEX_NAME = 'idx_organization');
SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_organization ON quotation_items (organization_id)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sales_orders
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sales_orders' AND INDEX_NAME = 'idx_organization');
SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_organization ON sales_orders (organization_id)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sales_order_items
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sales_order_items' AND INDEX_NAME = 'idx_organization');
SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_organization ON sales_order_items (organization_id)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- stock_movements
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'stock_movements' AND INDEX_NAME = 'idx_organization');
SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_organization ON stock_movements (organization_id)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- technical_proposals
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'technical_proposals' AND INDEX_NAME = 'idx_organization');
SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_organization ON technical_proposals (organization_id)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- technical_proposal_product_items
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'technical_proposal_product_items' AND INDEX_NAME = 'idx_organization');
SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_organization ON technical_proposal_product_items (organization_id)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- technical_proposal_service_items
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'technical_proposal_service_items' AND INDEX_NAME = 'idx_organization');
SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_organization ON technical_proposal_service_items (organization_id)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- contracts
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'contracts' AND INDEX_NAME = 'idx_organization');
SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_organization ON contracts (organization_id)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- contract_clauses
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'contract_clauses' AND INDEX_NAME = 'idx_organization');
SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_organization ON contract_clauses (organization_id)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- contract_product_items
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'contract_product_items' AND INDEX_NAME = 'idx_organization');
SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_organization ON contract_product_items (organization_id)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- contract_service_items
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'contract_service_items' AND INDEX_NAME = 'idx_organization');
SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_organization ON contract_service_items (organization_id)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- =============================================================================
-- 2. Unique constraints compostas (organization_id + coluna de negócio)
-- =============================================================================
--
-- As entidades Quotation, SalesOrder, TechnicalProposal e Contract declaram
-- @UniqueConstraint com columnNames = {"organization_uuid", ...} — resíduo
-- da era em que a coluna de tenant se chamava organization_uuid (BINARY(16)).
-- Hoje a coluna é organization_id (BIGINT), então o Hibernate criaria
-- constraints quebradas. Corrigimos aqui:
--
--   uk_quotation_org_number          (organization_id, number)
--   uk_sales_order_org_number        (organization_id, number)
--   uk_technical_proposal_org_code   (organization_id, prefix, sequence, year)
--   uk_contract_org_code             (organization_id, prefix, sequence, year)
--
-- Também recriamos as UKs escopadas por org para CPF/CNPJ (V22):
--   uk_companies_org_cnpj            (organization_id, cnpj)
--   uk_customers_org_cpf             (organization_id, cpf)
--   uk_sellers_org_cpf               (organization_id, cpf)
--   uk_suppliers_org_tax_id          (organization_id, tax_id)
--
-- E as UKs de prefixo de proposal/contract em organizations:
--   uk_organizations_proposal_prefix
--   uk_organizations_contract_prefix

-- ---------------------------------------------------------------------------
-- 2a. Drop das constraints com nome de coluna errado (organization_uuid)
--     que o Hibernate pode ter criado. Localizamos dinamicamente pelo nome
--     da constraint (começa com "uk_") e pela presença de "organization_uuid"
--     nas colunas.
-- ---------------------------------------------------------------------------

-- Helper: encontra UKs que contenham 'organization_uuid' como coluna
-- e as dropa. Isso cobre Quotation, SalesOrder, TechnicalProposal, Contract.

-- quotations: uk_quotation_org_number (ou nome gerado pelo Hibernate)
SET @uk_name = (
    SELECT s.INDEX_NAME
    FROM INFORMATION_SCHEMA.STATISTICS s
    WHERE s.TABLE_SCHEMA = DATABASE()
      AND s.TABLE_NAME = 'quotations'
      AND s.COLUMN_NAME = 'organization_uuid'
      AND s.NON_UNIQUE = 0
      AND s.INDEX_NAME <> 'PRIMARY'
    LIMIT 1
);
SET @sql = IF(@uk_name IS NOT NULL,
    CONCAT('ALTER TABLE quotations DROP INDEX `', @uk_name, '`'),
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sales_orders: uk_sales_order_org_number (ou nome gerado)
SET @uk_name = (
    SELECT s.INDEX_NAME
    FROM INFORMATION_SCHEMA.STATISTICS s
    WHERE s.TABLE_SCHEMA = DATABASE()
      AND s.TABLE_NAME = 'sales_orders'
      AND s.COLUMN_NAME = 'organization_uuid'
      AND s.NON_UNIQUE = 0
      AND s.INDEX_NAME <> 'PRIMARY'
    LIMIT 1
);
SET @sql = IF(@uk_name IS NOT NULL,
    CONCAT('ALTER TABLE sales_orders DROP INDEX `', @uk_name, '`'),
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- technical_proposals: uk_technical_proposal_org_code (ou nome gerado)
SET @uk_name = (
    SELECT s.INDEX_NAME
    FROM INFORMATION_SCHEMA.STATISTICS s
    WHERE s.TABLE_SCHEMA = DATABASE()
      AND s.TABLE_NAME = 'technical_proposals'
      AND s.COLUMN_NAME = 'organization_uuid'
      AND s.NON_UNIQUE = 0
      AND s.INDEX_NAME <> 'PRIMARY'
    LIMIT 1
);
SET @sql = IF(@uk_name IS NOT NULL,
    CONCAT('ALTER TABLE technical_proposals DROP INDEX `', @uk_name, '`'),
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- contracts: uk_contract_org_code (ou nome gerado)
SET @uk_name = (
    SELECT s.INDEX_NAME
    FROM INFORMATION_SCHEMA.STATISTICS s
    WHERE s.TABLE_SCHEMA = DATABASE()
      AND s.TABLE_NAME = 'contracts'
      AND s.COLUMN_NAME = 'organization_uuid'
      AND s.NON_UNIQUE = 0
      AND s.INDEX_NAME <> 'PRIMARY'
    LIMIT 1
);
SET @sql = IF(@uk_name IS NOT NULL,
    CONCAT('ALTER TABLE contracts DROP INDEX `', @uk_name, '`'),
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 2b. Cria as constraints corretas (com organization_id)
-- ---------------------------------------------------------------------------

-- uk_quotation_org_number (organization_id, number)
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'quotations' AND INDEX_NAME = 'uk_quotation_org_number');
SET @sql = IF(@has_idx = 0, 'CREATE UNIQUE INDEX uk_quotation_org_number ON quotations (organization_id, number)', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- uk_sales_order_org_number (organization_id, number)
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sales_orders' AND INDEX_NAME = 'uk_sales_order_org_number');
SET @sql = IF(@has_idx = 0, 'CREATE UNIQUE INDEX uk_sales_order_org_number ON sales_orders (organization_id, number)', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- uk_technical_proposal_org_code (organization_id, prefix, sequence, year)
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'technical_proposals' AND INDEX_NAME = 'uk_technical_proposal_org_code');
SET @sql = IF(@has_idx = 0, 'CREATE UNIQUE INDEX uk_technical_proposal_org_code ON technical_proposals (organization_id, prefix, sequence, year)', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- uk_contract_org_code (organization_id, prefix, sequence, year)
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'contracts' AND INDEX_NAME = 'uk_contract_org_code');
SET @sql = IF(@has_idx = 0, 'CREATE UNIQUE INDEX uk_contract_org_code ON contracts (organization_id, prefix, sequence, year)', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- uk_companies_org_cnpj (organization_id, cnpj)
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'companies' AND INDEX_NAME = 'uk_companies_org_cnpj');
SET @sql = IF(@has_idx = 0, 'CREATE UNIQUE INDEX uk_companies_org_cnpj ON companies (organization_id, cnpj)', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- uk_customers_org_cpf (organization_id, cpf)
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customers' AND INDEX_NAME = 'uk_customers_org_cpf');
SET @sql = IF(@has_idx = 0, 'CREATE UNIQUE INDEX uk_customers_org_cpf ON customers (organization_id, cpf)', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- uk_sellers_org_cpf (organization_id, cpf)
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sellers' AND INDEX_NAME = 'uk_sellers_org_cpf');
SET @sql = IF(@has_idx = 0, 'CREATE UNIQUE INDEX uk_sellers_org_cpf ON sellers (organization_id, cpf)', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- uk_suppliers_org_tax_id (organization_id, tax_id)
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'suppliers' AND INDEX_NAME = 'uk_suppliers_org_tax_id');
SET @sql = IF(@has_idx = 0, 'CREATE UNIQUE INDEX uk_suppliers_org_tax_id ON suppliers (organization_id, tax_id)', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- uk_organizations_proposal_prefix
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'organizations' AND INDEX_NAME = 'uk_organizations_proposal_prefix');
SET @sql = IF(@has_idx = 0, 'CREATE UNIQUE INDEX uk_organizations_proposal_prefix ON organizations (proposal_prefix)', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- uk_organizations_contract_prefix
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'organizations' AND INDEX_NAME = 'uk_organizations_contract_prefix');
SET @sql = IF(@has_idx = 0, 'CREATE UNIQUE INDEX uk_organizations_contract_prefix ON organizations (contract_prefix)', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- =============================================================================
-- 2c. Tabela service_templates (catálogo global de serviços)
-- =============================================================================
-- Entidade global (não organization-scoped), criada pelo Hibernate via
-- ddl-auto=update, mas garantimos a existência aqui para idempotência.
CREATE TABLE IF NOT EXISTS service_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- 3. Seed das Organizations Top Power
-- =============================================================================
-- Idempotente: INSERT ... WHERE NOT EXISTS por CNPJ.

INSERT INTO organizations (
    corporate_name,
    trade_name,
    cnpj,
    state_registration,
    municipal_registration,
    zip_code,
    street,
    number,
    district,
    city,
    state,
    status,
    proposal_prefix,
    contract_prefix,
    created_at,
    updated_at
)
SELECT 'TOP POWER ENGENHARIA LTDA ME',
       'TOP POWER ENGENHARIA',
       '13.433.616/0001-06',
       '671.137.811.110',
       '29764.01-6',
       '13170-700',
       'AVENIDA REBOUCAS',
       '4465',
       'RES. VECCON',
       'SUMARE',
       'SP',
       'ATIVO',
       'PT',
       'CT',
       now(),
       now()
WHERE NOT EXISTS (
    SELECT 1 FROM organizations WHERE cnpj = '13.433.616/0001-06'
);

INSERT INTO organizations (
    corporate_name,
    trade_name,
    cnpj,
    state_registration,
    municipal_registration,
    zip_code,
    street,
    number,
    district,
    city,
    state,
    status,
    proposal_prefix,
    contract_prefix,
    created_at,
    updated_at
)
SELECT 'TOP POWER MATERIAIS LTDA ME',
       'TOP POWER MATERIAIS',
       '59.530.698/0001-08',
       '671.700.534.116',
       '62965010',
       '13171-456',
       'RUA JOAO RAVAGNANI',
       '36',
       'JARDIM RESIDENCIAL RAVAGNANI',
       'SUMARE',
       'SP',
       'ATIVO',
       'PL',
       'CL',
       now(),
       now()
WHERE NOT EXISTS (
    SELECT 1 FROM organizations WHERE cnpj = '59.530.698/0001-08'
);
