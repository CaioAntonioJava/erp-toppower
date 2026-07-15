-- =============================================================================
-- V5__add_general_price_to_technical_proposal.sql
--
-- Adiciona a coluna general_price à tabela technical_proposals, um campo
-- opcional de preço geral de preenchimento livre pelo usuário. Não participa
-- de cálculos automáticos.
--
-- Usa INFORMATION_SCHEMA para verificar se a coluna já existe, garantindo
-- idempotência em bases já atualizadas.
-- =============================================================================

SET @dbname = DATABASE();
SET @tablename = 'technical_proposals';
SET @columnname = 'general_price';
SET @preparedStatement = (SELECT IF(
    (
        SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_NAME = @tablename
          AND COLUMN_NAME = @columnname
          AND TABLE_SCHEMA = @dbname
    ) > 0,
    'SELECT 1',
    'ALTER TABLE technical_proposals ADD COLUMN general_price DECIMAL(10, 2) NULL AFTER notes'
));
PREPARE stmt FROM @preparedStatement;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
