-- =============================================================================
-- V28__scope_quotation_sales_order_number_by_org.sql
--
-- Torna as constraints UNIQUE de número de proposta comercial (quotations.number)
-- e número de pedido de venda (sales_orders.number) ESCOPADAS por Organization
-- (organization_uuid + number), em vez de globais.
--
-- Motivo: a numeração de propostas comerciais e pedidos de venda passou a ser
-- independente por empresa (multi-empresa). Cada Organization reinicia sua
-- sequência (proposta em 1500, pedido em 1000), de modo que o mesmo número
-- passa a poder existir em empresas diferentes. A constraint UNIQUE global
-- (coluna `number` isolada) impede isso e deve ser substituída pela composta
-- (organization_uuid, number).
--
-- Colunas que PERMANECEM UNIQUE GLOBAL (intencional, fora do escopo):
--   organizations.cnpj, organizations.proposal_prefix, products.code,
--   companies.code, customers.code, profiles.cpf, profiles.email, users.email.
--
-- Ações (idempotentes, guardadas em INFORMATION_SCHEMA):
--   1) Localiza e DROPa o índice UNIQUE existente de coluna única
--      (gerado pelo Hibernate com nome imprevisível, ex.: uk_quotation_number
--      ou UK<hash>) em quotations.number e sales_orders.number.
--   2) Cria as novas constraints UNIQUE COMPOSTAS:
--        uk_quotation_org_number     (organization_uuid, number)
--        uk_sales_order_org_number   (organization_uuid, number)
--
-- Padrão PREPARE/EXECUTE dinâmico idêntico ao da V22, pois
-- `ALTER TABLE ... DROP INDEX IF EXISTS` não é suportado no MySQL.
--
-- Nota sobre o DROP do índice Hibernate: o nome gerado pelo Hibernate é
-- determinístico por base, mas imprevisível entre ambientes. Por isso
-- localizamos via INFORMATION_SCHEMA.STATISTICS: procuramos um índice
-- NON_UNIQUE=0 cuja ÚNICA coluna seja `number`, e dropamos pelo nome encontrado.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Helper: DROPa o índice UNIQUE de coluna única na tabela/coluna informadas.
-- Seta @drop_sql com 'ALTER TABLE <t> DROP INDEX <nome>' ou 'DO 0' (no-op).
-- -----------------------------------------------------------------------------

-- quotations.number
SET @uk_name = (
    SELECT s.INDEX_NAME
    FROM INFORMATION_SCHEMA.STATISTICS s
    WHERE s.TABLE_SCHEMA = DATABASE()
      AND s.TABLE_NAME = 'quotations'
      AND s.COLUMN_NAME = 'number'
      AND s.NON_UNIQUE = 0
      AND s.INDEX_NAME <> 'PRIMARY'
      AND s.INDEX_NAME <> 'uk_quotation_org_number'
      AND s.INDEX_NAME NOT LIKE 'uk_quotation_org_number%'
      AND (
          SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS s2
          WHERE s2.TABLE_SCHEMA = DATABASE()
            AND s2.TABLE_NAME = 'quotations'
            AND s2.INDEX_NAME = s.INDEX_NAME
        ) = 1
    LIMIT 1
);
SET @sql = IF(@uk_name IS NOT NULL,
    CONCAT('ALTER TABLE quotations DROP INDEX `', @uk_name, '`'),
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sales_orders.number
SET @uk_name = (
    SELECT s.INDEX_NAME
    FROM INFORMATION_SCHEMA.STATISTICS s
    WHERE s.TABLE_SCHEMA = DATABASE()
      AND s.TABLE_NAME = 'sales_orders'
      AND s.COLUMN_NAME = 'number'
      AND s.NON_UNIQUE = 0
      AND s.INDEX_NAME <> 'PRIMARY'
      AND s.INDEX_NAME <> 'uk_sales_order_org_number'
      AND s.INDEX_NAME NOT LIKE 'uk_sales_order_org_number%'
      AND (
          SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS s2
          WHERE s2.TABLE_SCHEMA = DATABASE()
            AND s2.TABLE_NAME = 'sales_orders'
            AND s2.INDEX_NAME = s.INDEX_NAME
        ) = 1
    LIMIT 1
);
SET @sql = IF(@uk_name IS NOT NULL,
    CONCAT('ALTER TABLE sales_orders DROP INDEX `', @uk_name, '`'),
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- 2. Cria as novas constraints UNIQUE COMPOSTAS (organization_uuid + number).
--    Idempotente: só cria se o índice ainda não existir.
-- -----------------------------------------------------------------------------

-- uk_quotation_org_number (organization_uuid, number)
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'quotations'
      AND INDEX_NAME = 'uk_quotation_org_number');
SET @sql = IF(@has_idx = 0,
    'CREATE UNIQUE INDEX uk_quotation_org_number ON quotations (organization_uuid, number)',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- uk_sales_order_org_number (organization_uuid, number)
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sales_orders'
      AND INDEX_NAME = 'uk_sales_order_org_number');
SET @sql = IF(@has_idx = 0,
    'CREATE UNIQUE INDEX uk_sales_order_org_number ON sales_orders (organization_uuid, number)',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;