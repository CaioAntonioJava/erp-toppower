-- Migration: adiciona a coluna `payment_condition` (condição de pagamento)
-- à tabela `contracts`. Campo opcional (NULLABLE), VARCHAR(50) — armazena o
-- nome do enum br.com.toppower.erp_toppower.sales.quotation.enums.PaymentCondition,
-- o mesmo domínio reutilizado por quotation/sales-order/technical-proposal.
-- O Hibernate (ddl-auto=update) roda ANTES deste script e já pode ter criado a
-- coluna; neste caso o ALTER é no-op graças ao guard INFORMATION_SCHEMA.

SET @has = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'contracts' AND COLUMN_NAME = 'payment_condition');
SET @sql = IF(@has = 0,
    'ALTER TABLE contracts ADD COLUMN payment_condition VARCHAR(50) NULL AFTER price',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;