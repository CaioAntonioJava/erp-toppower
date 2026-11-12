-- =============================================================================
-- V11__contract_finances.sql
--
-- Módulo Financeiro de Contratos (contractfinances):
--   1. contract_finance_controls — registro de controle por contrato
--      (código comercial, cliente PF/PJ, data de vigência e valor do
--      contrato), criado automaticamente pelo ContractService na criação
--      do contrato e sincronizado na alteração. Um controle por contrato
--      dentro da organização (unique organization_id + contract_id).
--   2. contract_finance_entries — lançamentos (gastos/recebimentos)
--      vinculados ao controle: natureza (RECEBIDO, MATERIAL, COMBUSTIVEL,
--      REFEICAO, PEDAGIO, TERCEIROS, OUTROS, SALARIOS), data, valor e
--      descrição. Soft delete marca status INATIVO.
--   3. Backfill idempotente: cria controles para todos os contratos que
--      ainda não têm um (módulo lançado depois do contract), resolvendo o
--      nome do cliente de customers (PF) ou companies (PJ,
--      trade_name -> legal_name).
--
-- Idempotente: spring.sql.init.mode=always roda a cada boot. Os CREATE
-- TABLE IF NOT EXISTS evitam erro se as tabelas já existirem (criadas
-- pelo ddl-auto=update); os guards via INFORMATION_SCHEMA adicionam os
-- índices/constraints apenas quando ausentes; o INSERT usa NOT EXISTS.
-- =============================================================================

CREATE TABLE IF NOT EXISTS contract_finance_controls (
    id BIGINT NOT NULL AUTO_INCREMENT,
    organization_id BIGINT NULL,
    contract_id BIGINT NOT NULL,
    contract_code VARCHAR(30) NOT NULL,
    client_type VARCHAR(20) NULL,
    client_code VARCHAR(50) NULL,
    client_name VARCHAR(200) NULL,
    contract_date DATE NULL,
    contract_value DECIMAL(12,2) NULL,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    created_by VARCHAR(255) NULL,
    updated_by VARCHAR(255) NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS contract_finance_entries (
    id BIGINT NOT NULL AUTO_INCREMENT,
    organization_id BIGINT NULL,
    control_id BIGINT NOT NULL,
    type VARCHAR(30) NOT NULL,
    entry_date DATE NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    description VARCHAR(500) NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    created_by VARCHAR(255) NULL,
    updated_by VARCHAR(255) NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Unique: um controle por contrato por organização (instalações em que a
-- tabela já existia sem a constraint).
SET @has_uk = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'contract_finance_controls'
      AND INDEX_NAME = 'uk_cfin_control_org_contract');
SET @sql = IF(@has_uk = 0,
    'ALTER TABLE contract_finance_controls ADD CONSTRAINT uk_cfin_control_org_contract UNIQUE (organization_id, contract_id)',
    'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Índice de busca por código do contrato.
SET @has_idx_code = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'contract_finance_controls'
      AND INDEX_NAME = 'idx_cfin_control_code');
SET @sql = IF(@has_idx_code = 0,
    'ALTER TABLE contract_finance_controls ADD INDEX idx_cfin_control_code (contract_code)',
    'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Backfill: cria controles para contratos sem controle (nomes de cliente
-- resolvidos de customers PF / companies PJ em tempo de migração).
INSERT INTO contract_finance_controls
    (organization_id, contract_id, contract_code, client_type, client_code,
     client_name, contract_date, contract_value, created_at, updated_at)
SELECT
    c.organization_id,
    c.id,
    CONCAT(c.prefix, '-', LPAD(c.sequence, 3, '0'), '-', c.year),
    CASE WHEN c.customer_id IS NOT NULL THEN 'CUSTOMER' ELSE 'COMPANY' END,
    CASE
        WHEN c.customer_id IS NOT NULL THEN cu.code
        ELSE co.code
    END,
    CASE
        WHEN c.customer_id IS NOT NULL THEN cu.name
        ELSE COALESCE(co.trade_name, co.legal_name)
    END,
    c.validity_date,
    c.price,
    NOW(6),
    NOW(6)
FROM contracts c
LEFT JOIN customers cu ON cu.id = c.customer_id
LEFT JOIN companies co ON co.id = c.company_id
WHERE NOT EXISTS (
    SELECT 1 FROM contract_finance_controls f
     WHERE f.contract_id = c.id
       AND f.organization_id = c.organization_id
);
