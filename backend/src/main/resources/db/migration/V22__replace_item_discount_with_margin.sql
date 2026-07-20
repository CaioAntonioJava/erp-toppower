-- Substitui o desconto por item pela margem de lucro por item em propostas
-- (quotation_items) e pedidos de venda (sales_order_items). A margem por item,
-- quando informada, sobrescreve a margem do cabeçalho, permitindo margens
-- diferentes por produto em uma mesma lista.
--
-- Convenção do projeto: scripts idempotentes via INFORMATION_SCHEMA +
-- PREPARE/EXECUTE (este MySQL não suporta ADD/DROP COLUMN IF EXISTS). O
-- ddl-auto=update adiciona colunas mas nunca dropa, então o DROP precisa
-- ser feito explicitamente aqui.
--
-- Em technical_proposal_product_items apenas removemos o desconto por item
-- (sem adicionar margem por item -- o módulo não tem conceito de margem).

-- 1. Adiciona profit_margin em quotation_items.
SET @has_qi_margin := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'quotation_items'
      AND COLUMN_NAME = 'profit_margin');
SET @sql := IF(@has_qi_margin = 0,
    'ALTER TABLE quotation_items ADD COLUMN profit_margin DECIMAL(5,2) NULL',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. Adiciona profit_margin em sales_order_items.
SET @has_soi_margin := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sales_order_items'
      AND COLUMN_NAME = 'profit_margin');
SET @sql := IF(@has_soi_margin = 0,
    'ALTER TABLE sales_order_items ADD COLUMN profit_margin DECIMAL(5,2) NULL',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3. Torna quotations.profit_margin nullable (a margem do cabeçalho deixa de
--    ser obrigatória quando algum item tem margem própria).
SET @q_margin_nullable := (SELECT IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'quotations'
      AND COLUMN_NAME = 'profit_margin');
SET @sql := IF(@q_margin_nullable = 'NO',
    'ALTER TABLE quotations MODIFY COLUMN profit_margin DECIMAL(5,2) NULL',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4. Remove discount e discount_type de quotation_items.
SET @has_qi_discount := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'quotation_items'
      AND COLUMN_NAME = 'discount');
SET @sql := IF(@has_qi_discount > 0,
    'ALTER TABLE quotation_items DROP COLUMN discount',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_qi_discount_type := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'quotation_items'
      AND COLUMN_NAME = 'discount_type');
SET @sql := IF(@has_qi_discount_type > 0,
    'ALTER TABLE quotation_items DROP COLUMN discount_type',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 5. Remove discount e discount_type de sales_order_items.
SET @has_soi_discount := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sales_order_items'
      AND COLUMN_NAME = 'discount');
SET @sql := IF(@has_soi_discount > 0,
    'ALTER TABLE sales_order_items DROP COLUMN discount',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_soi_discount_type := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sales_order_items'
      AND COLUMN_NAME = 'discount_type');
SET @sql := IF(@has_soi_discount_type > 0,
    'ALTER TABLE sales_order_items DROP COLUMN discount_type',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 6. Remove discount e discount_type de technical_proposal_product_items.
SET @has_tppi_discount := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'technical_proposal_product_items'
      AND COLUMN_NAME = 'discount');
SET @sql := IF(@has_tppi_discount > 0,
    'ALTER TABLE technical_proposal_product_items DROP COLUMN discount',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_tppi_discount_type := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'technical_proposal_product_items'
      AND COLUMN_NAME = 'discount_type');
SET @sql := IF(@has_tppi_discount_type > 0,
    'ALTER TABLE technical_proposal_product_items DROP COLUMN discount_type',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;