-- Tabelas de parcelas programadas de contas a receber (módulo receivable).
-- Convenção do projeto: PK BIGINT AUTO_INCREMENT, sem FKs físicas,
-- auditoria via BaseEntity (criada pelo Hibernate ddl-auto=update).
-- O Hibernate criará as colunas de auditoria (created_at, updated_at,
-- created_by, updated_by); este script garante apenas a tabela, os
-- índices, a unicidade, a coluna installment_id em payments, a coluna
-- installments_count em accounts_receivable e o backfill de parcela
-- única para contas existentes (modelo anterior sem parcelas).

-- Tabela de parcelas programadas de contas a receber.
CREATE TABLE IF NOT EXISTS accounts_receivable_installments (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    receivable_id     BIGINT        NOT NULL,
    installment_number INT          NOT NULL,
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

SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'accounts_receivable_installments' AND INDEX_NAME = 'idx_receivable_installment_receivable'); SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_receivable_installment_receivable ON accounts_receivable_installments (receivable_id)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'accounts_receivable_installments' AND INDEX_NAME = 'idx_receivable_installment_due_date'); SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_receivable_installment_due_date ON accounts_receivable_installments (due_date)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'accounts_receivable_installments' AND INDEX_NAME = 'idx_receivable_installment_status'); SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_receivable_installment_status ON accounts_receivable_installments (status)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Unique constraint (receivable_id, installment_number) — idempotente.
SET @has_uk = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'accounts_receivable_installments' AND INDEX_NAME = 'uk_receivable_installment'); SET @sql = IF(@has_uk = 0, 'CREATE UNIQUE INDEX uk_receivable_installment ON accounts_receivable_installments (receivable_id, installment_number)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Coluna installment_id em accounts_receivable_payments (nullable para
-- pagamentos antigos sem parcela vinculada).
SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'accounts_receivable_payments' AND COLUMN_NAME = 'installment_id'); SET @sql = IF(@has_col = 0, 'ALTER TABLE accounts_receivable_payments ADD COLUMN installment_id BIGINT NULL', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'accounts_receivable_payments' AND INDEX_NAME = 'idx_receivable_payment_installment'); SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_receivable_payment_installment ON accounts_receivable_payments (installment_id)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Coluna installments_count em accounts_receivable (default 1 = à vista).
SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'accounts_receivable' AND COLUMN_NAME = 'installments_count'); SET @sql = IF(@has_col = 0, 'ALTER TABLE accounts_receivable ADD COLUMN installments_count INT NOT NULL DEFAULT 1', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Backfill: para cada conta a receber existente sem parcela, cria 1
-- parcela (nº 1) com o valor/vencimento/status atuais. Contas novas
-- (criadas após esta migration) já terão sua(s) parcela(s) criadas pelo
-- service. Idempotente via WHERE NOT EXISTS.
INSERT INTO accounts_receivable_installments
    (receivable_id, installment_number, amount, due_date, paid_amount,
     status, payment_date, created_at, updated_at)
SELECT
    r.id, 1, r.value, r.due_date, r.paid_amount, r.status, r.payment_date,
    COALESCE(r.created_at, NOW(6)), COALESCE(r.updated_at, NOW(6))
FROM accounts_receivable r
WHERE NOT EXISTS (
    SELECT 1 FROM accounts_receivable_installments i
    WHERE i.receivable_id = r.id
);