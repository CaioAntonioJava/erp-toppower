-- Tabelas de contas a pagar (módulo payable).
-- Convenção do projeto: PK BIGINT AUTO_INCREMENT, sem FKs físicas,
-- coluna organization_id para isolamento multi-tenant (o
-- OrganizationEntityListener preenche no persist e o
-- OrganizationFilterAspect escopa queries JPQL/Criteria).
-- O Hibernate (ddl-auto=update) criará as colunas de auditoria (created_at,
-- updated_at, created_by, updated_by) e organization_id; este script garante
-- apenas a tabela, os índices e unicidade.
CREATE TABLE IF NOT EXISTS accounts_payable (
    id                       BIGINT        NOT NULL AUTO_INCREMENT,
    organization_id          BIGINT        NULL,
    description              VARCHAR(300)  NOT NULL,
    value                    DECIMAL(12, 2) NOT NULL,
    paid_amount              DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    issue_date               DATE          NOT NULL,
    due_date                 DATE          NOT NULL,
    status                   VARCHAR(20)   NOT NULL DEFAULT 'ABERTO',
    source_type              VARCHAR(25)   NOT NULL,
    supplier_id              BIGINT        NOT NULL,
    boleto_id                BIGINT        NULL,
    purchase_invoice_id      BIGINT        NULL,
    purchase_invoice_number  VARCHAR(30)   NULL,
    payment_condition        VARCHAR(50)   NULL,
    installments_count       INT           NOT NULL DEFAULT 1,
    payment_date             DATE          NULL,
    created_at               DATETIME(6)   NOT NULL,
    updated_at               DATETIME(6)   NOT NULL,
    created_by               VARCHAR(100)  NULL,
    updated_by               VARCHAR(100)  NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Índices auxiliares (todos idempotentes).
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'accounts_payable' AND INDEX_NAME = 'idx_payable_status'); SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_payable_status ON accounts_payable (status)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'accounts_payable' AND INDEX_NAME = 'idx_payable_due_date'); SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_payable_due_date ON accounts_payable (due_date)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'accounts_payable' AND INDEX_NAME = 'idx_payable_supplier'); SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_payable_supplier ON accounts_payable (supplier_id)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'accounts_payable' AND INDEX_NAME = 'idx_payable_source_type'); SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_payable_source_type ON accounts_payable (source_type)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'accounts_payable' AND INDEX_NAME = 'idx_payable_boleto'); SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_payable_boleto ON accounts_payable (boleto_id)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'accounts_payable' AND INDEX_NAME = 'idx_payable_purchase_invoice'); SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_payable_purchase_invoice ON accounts_payable (purchase_invoice_id)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'accounts_payable' AND INDEX_NAME = 'idx_payable_organization_id'); SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_payable_organization_id ON accounts_payable (organization_id)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Tabela de parcelas programadas de contas a pagar.
CREATE TABLE IF NOT EXISTS accounts_payable_installments (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    payable_id        BIGINT        NOT NULL,
    installment_number INT           NOT NULL,
    amount            DECIMAL(12, 2) NOT NULL,
    due_date          DATE          NOT NULL,
    paid_amount       DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    status            VARCHAR(20)   NOT NULL DEFAULT 'ABERTO',
    payment_date      DATE          NULL,
    created_at        DATETIME(6)   NOT NULL,
    updated_at        DATETIME(6)   NOT NULL,
    created_by        VARCHAR(100)  NULL,
    updated_by        VARCHAR(100)  NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'accounts_payable_installments' AND INDEX_NAME = 'idx_payable_installment_payable'); SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_payable_installment_payable ON accounts_payable_installments (payable_id)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'accounts_payable_installments' AND INDEX_NAME = 'idx_payable_installment_due_date'); SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_payable_installment_due_date ON accounts_payable_installments (due_date)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'accounts_payable_installments' AND INDEX_NAME = 'idx_payable_installment_status'); SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_payable_installment_status ON accounts_payable_installments (status)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Unique constraint (payable_id, installment_number) — idempotente.
SET @has_uk = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'accounts_payable_installments' AND INDEX_NAME = 'uk_payable_installment'); SET @sql = IF(@has_uk = 0, 'CREATE UNIQUE INDEX uk_payable_installment ON accounts_payable_installments (payable_id, installment_number)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Tabela de pagamentos avulsos de contas a pagar.
CREATE TABLE IF NOT EXISTS accounts_payable_payments (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    payable_id     BIGINT        NOT NULL,
    installment_id BIGINT        NOT NULL,
    amount         DECIMAL(12, 2) NOT NULL,
    payment_date   DATE          NOT NULL,
    notes          VARCHAR(500)  NULL,
    created_at     DATETIME(6)   NOT NULL,
    updated_at     DATETIME(6)   NOT NULL,
    created_by     VARCHAR(100)  NULL,
    updated_by     VARCHAR(100)  NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'accounts_payable_payments' AND INDEX_NAME = 'idx_payable_payment_payable'); SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_payable_payment_payable ON accounts_payable_payments (payable_id)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'accounts_payable_payments' AND INDEX_NAME = 'idx_payable_payment_installment'); SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_payable_payment_installment ON accounts_payable_payments (installment_id)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'accounts_payable_payments' AND INDEX_NAME = 'idx_payable_payment_date'); SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_payable_payment_date ON accounts_payable_payments (payment_date)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;