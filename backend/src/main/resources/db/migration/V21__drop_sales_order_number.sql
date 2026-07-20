-- Remove a coluna `number` de sales_orders, que foi substituída por
-- prefix + sequence + year na V19. A entidade Java não mapeia mais
-- esta coluna, causando erro "Field 'number' doesn't have a default value"
-- ao inserir novos pedidos.

SET @has_number := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sales_orders'
      AND COLUMN_NAME = 'number');
SET @sql := IF(@has_number > 0,
    'ALTER TABLE sales_orders DROP COLUMN number',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
