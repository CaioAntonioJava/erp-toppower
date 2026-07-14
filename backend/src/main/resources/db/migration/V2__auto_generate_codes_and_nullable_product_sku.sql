-- Migration V2: ajustes estruturais para suportar a auto-geração de códigos.
--
-- Alterações:
--   1. `products.code` deixa de ser obrigatório (SKU agora é opcional).
--   2. Backfill de `companies.code` para o padrão `EMP` + sequência de 6 dígitos.
--      (qualquer código que não esteja nesse padrão é substituído).
--   3. Backfill de `customers.code` para o padrão `CLI` + sequência de 6 dígitos.
--      (qualquer código que não esteja nesse padrão é substituído).
--
-- Estratégia de numeração: usamos ROW_NUMBER() ordenado por `created_at, id`
-- para que o primeiro registro cadastrado receba `...000001` e assim por diante.
-- Isso preserva uma ordem cronológica razoável sem depender de auto-increment.
--
-- O auto-increment real é feito em Java pelo CompanyService/CustomerService a
-- partir do MAX(code) com o prefixo correspondente — esta migration garante
-- apenas que o schema existente já obedeça ao novo formato.

-- 1) products.code: agora permite NULL.
ALTER TABLE products MODIFY COLUMN code VARCHAR(50) NULL;

-- 2) Backfill de companies.code → EMP000001, EMP000002, ...
UPDATE companies c
JOIN (
    SELECT
        id,
        CONCAT('EMP', LPAD(ROW_NUMBER() OVER (ORDER BY created_at, id), 6, '0')) AS new_code
    FROM companies
) seq ON seq.id = c.id
SET c.code = seq.new_code
WHERE c.code NOT LIKE 'EMP%'
   OR c.code NOT REGEXP '^EMP[0-9]{6}$';

-- 3) Backfill de customers.code → CLI000001, CLI000002, ...
UPDATE customers c
JOIN (
    SELECT
        id,
        CONCAT('CLI', LPAD(ROW_NUMBER() OVER (ORDER BY created_at, id), 6, '0')) AS new_code
    FROM customers
) seq ON seq.id = c.id
SET c.code = seq.new_code
WHERE c.code NOT LIKE 'CLI%'
   OR c.code NOT REGEXP '^CLI[0-9]{6}$';
