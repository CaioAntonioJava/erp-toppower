-- =============================================================================
-- V9__add_product_fiscal_fields.sql
--
-- Adiciona os campos fiscais (Simples Nacional) à tabela products. Embora o
-- Hibernate (ddl-auto=update) crie as colunas a partir da entidade Product, este
-- script garante:
--   - colunas criadas idempotentemente (mesmo em bases sem ddl-auto=update);
--   - defaults para produtos legados já cadastrados (origem, csosn, cstIpi,
--     cstPis, cstCofins);
--   - índice idx_products_ncm para acelerar busca/filtro por NCM.
--
-- O NCM é nullable no DB (produtos legados não o tinham); a obrigatoriedade é
-- garantida no DTO de criação (@NotBlank/@Pattern). Os demais campos fiscais
-- são nullable por natureza (opcionais no Simples Nacional).
--
-- Idempotente: cada coluna é guardada por INFORMATION_SCHEMA.COLUMNS e o
-- índice por INFORMATION_SCHEMA.STATISTICS.
-- =============================================================================

-- helper: verifica existência de coluna
SET @dbname = DATABASE();
SET @tablename = 'products';

-- ncm VARCHAR(8) NULL
SET @preparedStatement = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'ncm') > 0,
    'SELECT 1',
    'ALTER TABLE products ADD COLUMN ncm VARCHAR(8) NULL'
));
PREPARE stmt FROM @preparedStatement; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- origem VARCHAR(30) NULL DEFAULT 'NACIONAL'
SET @preparedStatement = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'origem') > 0,
    'SELECT 1',
    'ALTER TABLE products ADD COLUMN origem VARCHAR(30) NULL DEFAULT ''NACIONAL'''
));
PREPARE stmt FROM @preparedStatement; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- codigo_barras VARCHAR(14) NULL
SET @preparedStatement = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'codigo_barras') > 0,
    'SELECT 1',
    'ALTER TABLE products ADD COLUMN codigo_barras VARCHAR(14) NULL'
));
PREPARE stmt FROM @preparedStatement; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- cest VARCHAR(7) NULL
SET @preparedStatement = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'cest') > 0,
    'SELECT 1',
    'ALTER TABLE products ADD COLUMN cest VARCHAR(7) NULL'
));
PREPARE stmt FROM @preparedStatement; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ex_tipi VARCHAR(2) NULL
SET @preparedStatement = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'ex_tipi') > 0,
    'SELECT 1',
    'ALTER TABLE products ADD COLUMN ex_tipi VARCHAR(2) NULL'
));
PREPARE stmt FROM @preparedStatement; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- peso_liquido DECIMAL(12,4) NULL
SET @preparedStatement = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'peso_liquido') > 0,
    'SELECT 1',
    'ALTER TABLE products ADD COLUMN peso_liquido DECIMAL(12, 4) NULL'
));
PREPARE stmt FROM @preparedStatement; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- peso_bruto DECIMAL(12,4) NULL
SET @preparedStatement = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'peso_bruto') > 0,
    'SELECT 1',
    'ALTER TABLE products ADD COLUMN peso_bruto DECIMAL(12, 4) NULL'
));
PREPARE stmt FROM @preparedStatement; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- csosn VARCHAR(3) NULL DEFAULT '102'
SET @preparedStatement = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'csosn') > 0,
    'SELECT 1',
    'ALTER TABLE products ADD COLUMN csosn VARCHAR(3) NULL DEFAULT ''102'''
));
PREPARE stmt FROM @preparedStatement; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- aliquota_icms_st DECIMAL(5,2) NULL
SET @preparedStatement = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'aliquota_icms_st') > 0,
    'SELECT 1',
    'ALTER TABLE products ADD COLUMN aliquota_icms_st DECIMAL(5, 2) NULL'
));
PREPARE stmt FROM @preparedStatement; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- mva_st DECIMAL(5,2) NULL
SET @preparedStatement = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'mva_st') > 0,
    'SELECT 1',
    'ALTER TABLE products ADD COLUMN mva_st DECIMAL(5, 2) NULL'
));
PREPARE stmt FROM @preparedStatement; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- cst_ipi VARCHAR(2) NULL DEFAULT '99'
SET @preparedStatement = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'cst_ipi') > 0,
    'SELECT 1',
    'ALTER TABLE products ADD COLUMN cst_ipi VARCHAR(2) NULL DEFAULT ''99'''
));
PREPARE stmt FROM @preparedStatement; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- classe_enq_ipi VARCHAR(5) NULL
SET @preparedStatement = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'classe_enq_ipi') > 0,
    'SELECT 1',
    'ALTER TABLE products ADD COLUMN classe_enq_ipi VARCHAR(5) NULL'
));
PREPARE stmt FROM @preparedStatement; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- cst_pis VARCHAR(2) NULL DEFAULT '49'
SET @preparedStatement = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'cst_pis') > 0,
    'SELECT 1',
    'ALTER TABLE products ADD COLUMN cst_pis VARCHAR(2) NULL DEFAULT ''49'''
));
PREPARE stmt FROM @preparedStatement; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- cst_cofins VARCHAR(2) NULL DEFAULT '49'
SET @preparedStatement = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'cst_cofins') > 0,
    'SELECT 1',
    'ALTER TABLE products ADD COLUMN cst_cofins VARCHAR(2) NULL DEFAULT ''49'''
));
PREPARE stmt FROM @preparedStatement; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Índice para busca/filtro por NCM.
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND INDEX_NAME = 'idx_products_ncm');
SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_products_ncm ON products (ncm)', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;