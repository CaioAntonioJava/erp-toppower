-- Tabela de contas a receber (módulo receivable).
-- Convenção do projeto: PK BIGINT AUTO_INCREMENT, sem FKs físicas,
-- coluna organization_id para isolamento multi-tenant (o
-- OrganizationEntityListener preenche no persist e o
-- OrganizationFilterAspect escopa queries JPQL/Criteria).
-- O Hibernate (ddl-auto=update) criará as colunas de auditoria (created_at,
-- updated_at, created_by, updated_by) e organization_id; este script garante
-- apenas a tabela, os índices e unicidade.
CREATE TABLE IF NOT EXISTS accounts_receivable (
    id                       BIGINT        NOT NULL AUTO_INCREMENT,
    organization_id          BIGINT        NULL,
    description              VARCHAR(300)  NOT NULL,
    value                    DECIMAL(12, 2) NOT NULL,
    paid_amount              DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    due_date                 DATE          NOT NULL,
    status                   VARCHAR(20)   NOT NULL DEFAULT 'ABERTO',
    source_type              VARCHAR(25)   NOT NULL,
    customer_id              BIGINT        NULL,
    company_id               BIGINT        NULL,
    payment_condition        VARCHAR(100)  NULL,
    sales_order_id           BIGINT        NULL,
    sales_order_number       BIGINT        NULL,
    technical_proposal_id    BIGINT        NULL,
    technical_proposal_code  VARCHAR(30)   NULL,
    contract_id              BIGINT        NULL,
    contract_code            VARCHAR(30)   NULL,
    payment_date             DATE          NULL,
    created_at               DATETIME(6)   NOT NULL,
    updated_at               DATETIME(6)   NOT NULL,
    created_by               VARCHAR(100)  NULL,
    updated_by               VARCHAR(100)  NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Índices auxiliares (todos idempotentes).
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'accounts_receivable' AND INDEX_NAME = 'idx_receivable_status'); SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_receivable_status ON accounts_receivable (status)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'accounts_receivable' AND INDEX_NAME = 'idx_receivable_due_date'); SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_receivable_due_date ON accounts_receivable (due_date)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'accounts_receivable' AND INDEX_NAME = 'idx_receivable_customer'); SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_receivable_customer ON accounts_receivable (customer_id)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'accounts_receivable' AND INDEX_NAME = 'idx_receivable_company'); SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_receivable_company ON accounts_receivable (company_id)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'accounts_receivable' AND INDEX_NAME = 'idx_receivable_source_type'); SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_receivable_source_type ON accounts_receivable (source_type)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'accounts_receivable' AND INDEX_NAME = 'idx_receivable_sales_order'); SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_receivable_sales_order ON accounts_receivable (sales_order_id)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'accounts_receivable' AND INDEX_NAME = 'idx_receivable_technical_proposal'); SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_receivable_technical_proposal ON accounts_receivable (technical_proposal_id)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'accounts_receivable' AND INDEX_NAME = 'idx_receivable_contract'); SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_receivable_contract ON accounts_receivable (contract_id)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'accounts_receivable' AND INDEX_NAME = 'idx_receivable_organization_id'); SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_receivable_organization_id ON accounts_receivable (organization_id)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Tabela de pagamentos avulsos de contas a receber.
CREATE TABLE IF NOT EXISTS accounts_receivable_payments (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    receivable_id BIGINT        NOT NULL,
    amount        DECIMAL(12, 2) NOT NULL,
    payment_date  DATE          NOT NULL,
    notes         VARCHAR(500)  NULL,
    created_at    DATETIME(6)   NOT NULL,
    updated_at    DATETIME(6)   NOT NULL,
    created_by    VARCHAR(100)  NULL,
    updated_by    VARCHAR(100)  NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'accounts_receivable_payments' AND INDEX_NAME = 'idx_receivable_payment_receivable'); SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_receivable_payment_receivable ON accounts_receivable_payments (receivable_id)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'accounts_receivable_payments' AND INDEX_NAME = 'idx_receivable_payment_date'); SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_receivable_payment_date ON accounts_receivable_payments (payment_date)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;