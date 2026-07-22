-- =============================================================================
-- V2__supplier_email_nullable.sql
--
-- Torna a coluna email da tabela suppliers opcional (nullable), refletindo a
-- remoção da validação @NotBlank no SupplierCreateRequest e do nullable=false
-- na entidade Supplier. O Hibernate (ddl-auto=update) NÃO remove NOT NULL
-- automaticamente, por isso esta migration é necessária.
-- Idempotente: pode ser executada múltiplas vezes sem erro.
-- =============================================================================

-- MySQL 8 não suporta ALTER TABLE ... MODIFY COLUMN IF NOT EXISTS, então
-- verificamos se a coluna está NOT NULL antes de aplicar a alteração.
SET @is_nullable = (
    SELECT IS_NULLABLE
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'suppliers'
      AND COLUMN_NAME = 'email'
);

SET @sql = IF(
    @is_nullable = 'NO',
    'ALTER TABLE suppliers MODIFY COLUMN email VARCHAR(150) NULL',
    'DO 0'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;