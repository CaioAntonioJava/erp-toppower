-- Migration: adiciona a coluna `delivery_date` (data de entrega/conclusão)
-- à tabela `contracts`. Campo opcional (NULLABLE), preenchido automaticamente
-- quando o contrato transita para o status CONCLUIDO e limpo quando reaberto.
-- Idempotente: usa guard IF NOT EXISTS. O Hibernate (ddl-auto=update) roda
-- ANTES deste script e já pode ter criado a coluna; neste caso o ADD COLUMN
-- é no-op.

SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'contracts' AND COLUMN_NAME = 'delivery_date');
SET @sql = IF(@has_col = 0,
    'ALTER TABLE contracts ADD COLUMN delivery_date DATE NULL AFTER price',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;