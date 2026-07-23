-- ============================================================================
-- V4 — Chave de acesso única da NF-e + índice de tenant da tabela
-- product_supplier_codes (relação produto↔fornecedor↔cProd).
--
-- A tabela product_supplier_codes e a coluna purchase_invoice_access_key
-- em accounts_payable são criadas pelo Hibernate (ddl-auto=update) a partir
-- das entidades. Aqui adicionamos apenas o que o Hibernate NÃO gera:
-- índice único de domínio (org + access_key) e índice de tenant.
-- Tudo idempotente (padrão PREPARE/EXECUTE).
-- ============================================================================

-- uk_payable_org_access_key: uma Chave de Acesso da NF-e só pode gerar
-- uma conta a pagar ativa por organização (idempotência da importação).
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'accounts_payable'
      AND INDEX_NAME = 'uk_payable_org_access_key');
SET @sql = IF(@has_idx = 0,
    'CREATE UNIQUE INDEX uk_payable_org_access_key ON accounts_payable (organization_id, purchase_invoice_access_key)',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- idx_organization em product_supplier_codes (filtro multi-tenant).
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product_supplier_codes'
      AND INDEX_NAME = 'idx_organization');
SET @sql = IF(@has_idx = 0,
    'CREATE INDEX idx_organization ON product_supplier_codes (organization_id)',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;