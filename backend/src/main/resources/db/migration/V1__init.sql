-- =============================================================================
-- V1__init.sql
--
-- Migration única e consolidada para o ERP TopPower.
--
-- O sistema nunca foi para produção, então este script assume banco sempre
-- limpo: sem lógica de backfill, rename ou normalização de dados legados.
-- O Hibernate (ddl-auto=update) cria todas as tabelas e colunas a partir das
-- entidades JPA, incluindo os índices declarados via @Index e as unique
-- constraints declaradas via @UniqueConstraint. Este script complementa
-- apenas o que o Hibernate NÃO gera automaticamente:
--
--   1) Índices idx_organization em organization_id das tabelas org-scoped
--      (o Hibernate não cria índices em colunas de tenant sem @Index explícito).
--   2) Unique constraints de domínio (CNPJ/CPF/tax_id por organização, prefixos
--      únicos em organizations, descrição única de boleto por organização).
--   3) Índices auxiliares de domínio em tabelas que não declaram @Index
--      (boletos, boleto_attachments, products.ncm).
--   4) Seed das duas Organizations Top Power (idempotente).
--
-- Idempotente: todos os blocos usam INFORMATION_SCHEMA para verificar
-- existência antes de criar, pois spring.sql.init.mode=always roda a cada boot.
-- =============================================================================

-- =============================================================================
-- 1. Índices idx_organization nas tabelas organization-scoped
-- =============================================================================
-- O Hibernate cria a coluna organization_id (mapeada em OrganizationScopedEntity)
-- mas não cria índice sobre ela. O OrganizationFilterAspect escopa todas as
-- queries JPQL/Criteria por organization_id, então o índice é essencial para
-- performance do filtro multi-tenant.

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

-- =============================================================================
-- 2. Unique constraints de domínio (não declaradas nas entidades)
-- =============================================================================
-- Estas constraints não estão em @UniqueConstraint das entidades, então o
-- Hibernate não as cria. São regras de unicidade por organização (multi-tenant)
-- ou globais (prefixos de Organization).

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

-- uk_boletos_org_description (organization_id, description)
-- NOTA: este unique index foi removido na migration V3, pois boletos
-- podem ter a mesma descrição. Mantemos o bloco comentado para não
-- quebrar o boot em bancos com dados duplicados.
-- SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'boletos' AND INDEX_NAME = 'uk_boletos_org_description');
-- SET @sql = IF(@has_idx = 0, 'CREATE UNIQUE INDEX uk_boletos_org_description ON boletos (organization_id, description)', 'DO 0');
-- PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- =============================================================================
-- 3. Índices auxiliares de domínio (não declarados via @Index nas entidades)
-- =============================================================================
-- Boletos, boleto_attachments e products.ncm não declaram @Index na entidade,
-- então o Hibernate não cria os índices auxiliares. Demais tabelas de domínio
-- (contracts, receivables, payables, technical_proposal_conditions,
-- contract_clauses, etc.) já declaram seus índices via @Index e são criados
-- pelo Hibernate.

-- boletos: status, description, due_date, organization_id, supplier_id
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'boletos' AND INDEX_NAME = 'idx_boletos_status');
SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_boletos_status ON boletos (status)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'boletos' AND INDEX_NAME = 'idx_boletos_description');
SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_boletos_description ON boletos (description)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'boletos' AND INDEX_NAME = 'idx_boletos_due_date');
SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_boletos_due_date ON boletos (due_date)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'boletos' AND INDEX_NAME = 'idx_boletos_organization_id');
SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_boletos_organization_id ON boletos (organization_id)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'boletos' AND INDEX_NAME = 'idx_boletos_supplier_id');
SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_boletos_supplier_id ON boletos (supplier_id)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- boleto_attachments: boleto_id, organization_id
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'boleto_attachments' AND INDEX_NAME = 'idx_boleto_attachments_boleto_id');
SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_boleto_attachments_boleto_id ON boleto_attachments (boleto_id)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'boleto_attachments' AND INDEX_NAME = 'idx_boleto_attachments_organization_id');
SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_boleto_attachments_organization_id ON boleto_attachments (organization_id)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- products: índice para busca/filtro por NCM
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'products' AND INDEX_NAME = 'idx_products_ncm');
SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_products_ncm ON products (ncm)', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- =============================================================================
-- 3b. Limpeza de resíduos legados (constraints/colunas órfãs)
-- =============================================================================
-- O Hibernate (ddl-auto=update) nunca dropa constraints nem colunas, então
-- resíduos de versões anteriores da entidade Payable permanecem no banco
-- mesmo após a coluna document_number ter sido removida da entidade. A
-- constraint uk_ap_org_document_number impede INSERTs porque a coluna órfã
-- document_number não recebe valor (a entidade não a mapeia mais).

-- Dropa a unique constraint órfã uk_ap_org_document_number de accounts_payable.
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'accounts_payable' AND INDEX_NAME = 'uk_ap_org_document_number');
SET @sql = IF(@has_idx > 0, 'ALTER TABLE accounts_payable DROP INDEX uk_ap_org_document_number', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Dropa a coluna órfã document_number de accounts_payable (não mapeada na
-- entidade Payable; era NOT NULL sem default, o que faria falhar INSERTs).
SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'accounts_payable' AND COLUMN_NAME = 'document_number');
SET @sql = IF(@has_col > 0, 'ALTER TABLE accounts_payable DROP COLUMN document_number', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- =============================================================================
-- 4. Seed das Organizations Top Power
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