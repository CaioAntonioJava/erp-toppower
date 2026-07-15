-- =============================================================================
-- V6__add_category_and_template_to_service_item.sql
--
-- Adiciona as colunas category e service_template_id à tabela
-- technical_proposal_service_items, permitindo que o frontend restaure
-- o estado do formulário (categoria + template selecionado) no modo edição.
-- =============================================================================

SET @dbname = DATABASE();
SET @tablename = 'technical_proposal_service_items';

SET @col1 = 'category';
SET @preparedStatement1 = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_NAME = @tablename AND COLUMN_NAME = @col1 AND TABLE_SCHEMA = @dbname) > 0,
    'SELECT 1',
    'ALTER TABLE technical_proposal_service_items ADD COLUMN category VARCHAR(50) NULL'
));
PREPARE stmt1 FROM @preparedStatement1;
EXECUTE stmt1;
DEALLOCATE PREPARE stmt1;

SET @col2 = 'service_template_id';
SET @preparedStatement2 = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_NAME = @tablename AND COLUMN_NAME = @col2 AND TABLE_SCHEMA = @dbname) > 0,
    'SELECT 1',
    'ALTER TABLE technical_proposal_service_items ADD COLUMN service_template_id BIGINT NULL'
));
PREPARE stmt2 FROM @preparedStatement2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;
