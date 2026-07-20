-- Refatoração do código do pedido de venda: substitui a coluna `number`
-- (Bigint sequencial) por `prefix` + `sequence` + `year`, formando o
-- código formatado "PV-2800-2026". A sequência reseta por ano e é
-- independente por Organization (multi-empresa).
--
-- Convenção do projeto: PK BIGINT AUTO_INCREMENT, sem FKs físicas, scripts
-- idempotentes via INFORMATION_SCHEMA + PREPARE/EXECUTE (este MySQL não
-- suporta ADD COLUMN IF NOT EXISTS). A coluna `number` antiga é preservada
-- (ddl-auto=update não dropa colunas; apenas a entidade deixa de mapeá-la).

-- 1. Novas colunas de código prefixado em sales_orders (uma por vez,
--    com verificação de INFORMATION_SCHEMA).
SET @has_prefix := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sales_orders' AND COLUMN_NAME = 'prefix');
SET @sql := IF(@has_prefix = 0,
    'ALTER TABLE sales_orders ADD COLUMN prefix VARCHAR(10) NOT NULL DEFAULT ''PV''',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_sequence := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sales_orders' AND COLUMN_NAME = 'sequence');
SET @sql := IF(@has_sequence = 0,
    'ALTER TABLE sales_orders ADD COLUMN sequence BIGINT NOT NULL DEFAULT 0',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_year := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sales_orders' AND COLUMN_NAME = 'year');
SET @sql := IF(@has_year = 0,
    'ALTER TABLE sales_orders ADD COLUMN year INT NOT NULL DEFAULT 2026',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. Backfill: prefixo fixo "PV"; sequência = number antigo; ano = ano
--    de emissão (order_date). Roda em todo boot (idempotente — só afeta
--    linhas onde sequence ainda é 0 e number não é nulo).
UPDATE sales_orders
   SET prefix = 'PV',
       sequence = number,
       year = YEAR(order_date)
 WHERE number IS NOT NULL
   AND sequence = 0;

-- 3. Substitui a UK: dropa a antiga (organization_id, number) e cria a
--    nova (organization_id, prefix, sequence, year).
SET @uk_old_exists := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sales_orders'
      AND INDEX_NAME = 'uk_sales_order_org_number');
SET @sql := IF(@uk_old_exists > 0,
    'ALTER TABLE sales_orders DROP INDEX uk_sales_order_org_number',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @uk_new_exists := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sales_orders'
      AND INDEX_NAME = 'uk_sales_order_org_code');
SET @sql := IF(@uk_new_exists = 0,
    'CREATE UNIQUE INDEX uk_sales_order_org_code ON sales_orders (organization_id, prefix, sequence, year)',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4. Snapshot do código formatado do pedido nas contas a receber
--    (espelha technical_proposal_code / contract_code).
SET @has_so_code := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'accounts_receivable'
      AND COLUMN_NAME = 'sales_order_code');
SET @sql := IF(@has_so_code = 0,
    'ALTER TABLE accounts_receivable ADD COLUMN sales_order_code VARCHAR(30) NULL',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 5. Alarga stock_movements.source_number de BIGINT para VARCHAR, para
--    abrigar o código formatado "PV-2800-2026" (ddl-auto=update não
--    alarga tipo de coluna, então fazemos explicitamente).
SET @source_number_type := (SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'stock_movements'
      AND COLUMN_NAME = 'source_number');
SET @sql := IF(@source_number_type = 'bigint',
    'ALTER TABLE stock_movements MODIFY COLUMN source_number VARCHAR(50) NULL',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;