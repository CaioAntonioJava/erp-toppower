-- Migration: adiciona a coluna `price` (preço do contrato) à tabela
-- `contracts`. Campo obrigatório (NOT NULL), monetário (DECIMAL(12,2)).
-- Como a coluna é NOT NULL e pode já existirem contratos sem preço, esta
-- migration adiciona a coluna como NULLABLE, preenche os registros
-- existentes com 0.00 e em seguida a torna NOT NULL — sequência idempotente
-- graças aos guards IF NOT EXISTS / recheio protegido por verificação.
-- O Hibernate (ddl-auto=update) roda ANTES deste script e já pode ter
-- criado a coluna; neste caso os passos de ADD COLUMN são no-op.

-- 1) Adiciona a coluna `price` como NULLABLE (se não existir).
SET @has_price = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'contracts' AND COLUMN_NAME = 'price');
SET @sql = IF(@has_price = 0,
    'ALTER TABLE contracts ADD COLUMN price DECIMAL(12,2) NULL AFTER validity_date',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) Preenche registros existentes sem preço com 0.00 (apenas quando a
--    coluna acabou de ser criada ou ainda há NULLs).
UPDATE contracts SET price = 0.00 WHERE price IS NULL;

-- 3) Torna a coluna NOT NULL (idempotente: ALTER ... MODIFY é seguro de
--    rodar novamente; o MySQL apenas revalida a constraint).
SET @sql = 'ALTER TABLE contracts MODIFY COLUMN price DECIMAL(12,2) NOT NULL';
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;