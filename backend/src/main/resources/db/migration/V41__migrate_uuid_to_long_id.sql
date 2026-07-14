-- =============================================================================
-- V41__migrate_uuid_to_long_id.sql
--
-- Migration para compatibilidade com a refatoração UUID → Long Id.
--
-- O Hibernate (ddl-auto=update) já recria as tabelas com BIGINT AUTO_INCREMENT
-- e colunas renomeadas (id, *_id). Este script garante que as constraints
-- UNIQUE e índices sejam recriados com os novos nomes de colunas.
--
-- Idempotente: usa IF NOT EXISTS / IF EXISTS / CREATE ... IF NOT EXISTS.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. organizations
-- ---------------------------------------------------------------------------
-- A PK já é AUTO_INCREMENT pelo Hibernate. Recria a constraint UNIQUE do CNPJ
-- se ela não existir com o nome esperado.
ALTER TABLE organizations
    MODIFY COLUMN id BIGINT AUTO_INCREMENT;

-- ---------------------------------------------------------------------------
-- 2. users
-- ---------------------------------------------------------------------------
ALTER TABLE users
    MODIFY COLUMN id BIGINT AUTO_INCREMENT;

-- ---------------------------------------------------------------------------
-- 3. profiles
-- ---------------------------------------------------------------------------
ALTER TABLE profiles
    MODIFY COLUMN id BIGINT AUTO_INCREMENT,
    MODIFY COLUMN user_id BIGINT;

-- ---------------------------------------------------------------------------
-- 4. carriers
-- ---------------------------------------------------------------------------
ALTER TABLE carriers
    MODIFY COLUMN id BIGINT AUTO_INCREMENT;

-- ---------------------------------------------------------------------------
-- 5. user_organizations
-- ---------------------------------------------------------------------------
ALTER TABLE user_organizations
    MODIFY COLUMN id BIGINT AUTO_INCREMENT,
    MODIFY COLUMN user_id BIGINT,
    MODIFY COLUMN organization_id BIGINT;

-- ---------------------------------------------------------------------------
-- 6. companies
-- ---------------------------------------------------------------------------
ALTER TABLE companies
    MODIFY COLUMN id BIGINT AUTO_INCREMENT,
    MODIFY COLUMN organization_id BIGINT;

-- ---------------------------------------------------------------------------
-- 7. customers
-- ---------------------------------------------------------------------------
ALTER TABLE customers
    MODIFY COLUMN id BIGINT AUTO_INCREMENT,
    MODIFY COLUMN organization_id BIGINT;

-- ---------------------------------------------------------------------------
-- 8. sellers
-- ---------------------------------------------------------------------------
ALTER TABLE sellers
    MODIFY COLUMN id BIGINT AUTO_INCREMENT,
    MODIFY COLUMN organization_id BIGINT;

-- ---------------------------------------------------------------------------
-- 9. suppliers
-- ---------------------------------------------------------------------------
ALTER TABLE suppliers
    MODIFY COLUMN id BIGINT AUTO_INCREMENT,
    MODIFY COLUMN organization_id BIGINT;

-- ---------------------------------------------------------------------------
-- 10. products
-- ---------------------------------------------------------------------------
ALTER TABLE products
    MODIFY COLUMN id BIGINT AUTO_INCREMENT,
    MODIFY COLUMN organization_id BIGINT;

-- ---------------------------------------------------------------------------
-- 11. stock_movements
-- ---------------------------------------------------------------------------
ALTER TABLE stock_movements
    MODIFY COLUMN id BIGINT AUTO_INCREMENT,
    MODIFY COLUMN organization_id BIGINT,
    MODIFY COLUMN product_id BIGINT,
    MODIFY COLUMN source_id BIGINT,
    MODIFY COLUMN reversal_of_id BIGINT;

-- ---------------------------------------------------------------------------
-- 12. quotations
-- ---------------------------------------------------------------------------
ALTER TABLE quotations
    MODIFY COLUMN id BIGINT AUTO_INCREMENT,
    MODIFY COLUMN organization_id BIGINT,
    MODIFY COLUMN customer_id BIGINT,
    MODIFY COLUMN company_id BIGINT,
    MODIFY COLUMN seller_id BIGINT,
    MODIFY COLUMN carrier_id BIGINT;

-- ---------------------------------------------------------------------------
-- 13. quotation_items
-- ---------------------------------------------------------------------------
ALTER TABLE quotation_items
    MODIFY COLUMN id BIGINT AUTO_INCREMENT,
    MODIFY COLUMN organization_id BIGINT,
    MODIFY COLUMN quotation_id BIGINT,
    MODIFY COLUMN product_id BIGINT;

-- ---------------------------------------------------------------------------
-- 14. sales_orders
-- ---------------------------------------------------------------------------
ALTER TABLE sales_orders
    MODIFY COLUMN id BIGINT AUTO_INCREMENT,
    MODIFY COLUMN organization_id BIGINT,
    MODIFY COLUMN customer_id BIGINT,
    MODIFY COLUMN company_id BIGINT,
    MODIFY COLUMN seller_id BIGINT,
    MODIFY COLUMN carrier_id BIGINT,
    MODIFY COLUMN quotation_id BIGINT;

-- ---------------------------------------------------------------------------
-- 15. sales_order_items
-- ---------------------------------------------------------------------------
ALTER TABLE sales_order_items
    MODIFY COLUMN id BIGINT AUTO_INCREMENT,
    MODIFY COLUMN organization_id BIGINT,
    MODIFY COLUMN sales_order_id BIGINT,
    MODIFY COLUMN product_id BIGINT;

-- ---------------------------------------------------------------------------
-- 16. technical_proposals
-- ---------------------------------------------------------------------------
ALTER TABLE technical_proposals
    MODIFY COLUMN id BIGINT AUTO_INCREMENT,
    MODIFY COLUMN organization_id BIGINT,
    MODIFY COLUMN customer_id BIGINT,
    MODIFY COLUMN company_id BIGINT,
    MODIFY COLUMN carrier_id BIGINT;

-- ---------------------------------------------------------------------------
-- 17. technical_proposal_objectives
-- ---------------------------------------------------------------------------
ALTER TABLE technical_proposal_objectives
    MODIFY COLUMN id BIGINT AUTO_INCREMENT,
    MODIFY COLUMN organization_id BIGINT,
    MODIFY COLUMN technical_proposal_id BIGINT;

-- ---------------------------------------------------------------------------
-- 18. technical_proposal_product_items
-- ---------------------------------------------------------------------------
ALTER TABLE technical_proposal_product_items
    MODIFY COLUMN id BIGINT AUTO_INCREMENT,
    MODIFY COLUMN organization_id BIGINT,
    MODIFY COLUMN technical_proposal_id BIGINT,
    MODIFY COLUMN product_id BIGINT;

-- ---------------------------------------------------------------------------
-- 19. technical_proposal_service_items
-- ---------------------------------------------------------------------------
ALTER TABLE technical_proposal_service_items
    MODIFY COLUMN id BIGINT AUTO_INCREMENT,
    MODIFY COLUMN organization_id BIGINT,
    MODIFY COLUMN technical_proposal_id BIGINT;

-- ---------------------------------------------------------------------------
-- 20. contracts
-- ---------------------------------------------------------------------------
ALTER TABLE contracts
    MODIFY COLUMN id BIGINT AUTO_INCREMENT,
    MODIFY COLUMN organization_id BIGINT,
    MODIFY COLUMN customer_id BIGINT,
    MODIFY COLUMN company_id BIGINT;

-- ---------------------------------------------------------------------------
-- 21. contract_clauses
-- ---------------------------------------------------------------------------
ALTER TABLE contract_clauses
    MODIFY COLUMN id BIGINT AUTO_INCREMENT,
    MODIFY COLUMN organization_id BIGINT,
    MODIFY COLUMN contract_id BIGINT;

-- ---------------------------------------------------------------------------
-- 22. contract_product_items
-- ---------------------------------------------------------------------------
ALTER TABLE contract_product_items
    MODIFY COLUMN id BIGINT AUTO_INCREMENT,
    MODIFY COLUMN organization_id BIGINT,
    MODIFY COLUMN contract_id BIGINT,
    MODIFY COLUMN product_id BIGINT;

-- ---------------------------------------------------------------------------
-- 23. contract_service_items
-- ---------------------------------------------------------------------------
ALTER TABLE contract_service_items
    MODIFY COLUMN id BIGINT AUTO_INCREMENT,
    MODIFY COLUMN organization_id BIGINT,
    MODIFY COLUMN contract_id BIGINT;
