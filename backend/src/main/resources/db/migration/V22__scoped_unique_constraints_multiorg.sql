-- =============================================================================
-- V22__scoped_unique_constraints_multiorg.sql
--
-- Torna as constraints UNIQUE de CPF/CNPJ/tax_id ESCOPADAS por Organization
-- (organization_uuid + coluna), em vez de globais. Permite que duas empresas
-- diferentes cadastrem o mesmo cliente/vendedor/fornecedor, mas proíbe a
-- duplicidade DENTRO da mesma empresa.
--
-- Motivo: as entidades Company/Customer/Seller/Supplier herdam de
-- OrganizationScopedEntity (isolamento lógico via coluna organization_uuid +
-- filtro Hibernate), mas as colunas cnpj/cpf/tax_id foram declaradas com
-- @Column(unique = true), gerando índices UNIQUE GLOBAIS no schema. Isso
-- impede a segunda empresa de cadastrar o mesmo CPF/CNPJ da primeira,
-- violando o isolamento multiempresa.
--
-- Colunas que PERMANECEM UNIQUE GLOBAL (intencional):
--   companies.code, customers.code — numeração sequencial contínua entre
--   todas as empresas (decisão de arquitetura; ver findMaxCodeByPrefix
--   nativo em CompanyRepository/CustomerRepository).
--   organizations.cnpj — o CNPJ da empresa-tenant é único no sistema.
--   products.code — SKU informado pelo usuário.
--   profiles.cpf, profiles.email — perfil é do usuário (global), não da empresa.
--
-- Ações (idempotentes, guardadas em INFORMATION_SCHEMA):
--   1) Localiza e DROPa o índice UNIQUE existente (nome imprevisível gerado
--      pelo Hibernate, ex.: UKm1bj83lpw9...) em:
--        companies.cnpj, customers.cpf, sellers.cpf, suppliers.tax_id
--   2) Cria as novas constraints UNIQUE COMPOSTAS:
--        uk_companies_org_cnpj   (organization_uuid, cnpj)
--        uk_customers_org_cpf     (organization_uuid, cpf)
--        uk_sellers_org_cpf       (organization_uuid, cpf)
--        uk_suppliers_org_tax_id  (organization_uuid, tax_id)
--
-- Padrão PREPARE/EXECUTE dinâmico idêntico ao da V16/V18, pois
-- `ALTER TABLE ... DROP INDEX IF EXISTS` não é suportado no MySQL.
--
-- Nota sobre o DROP do índice Hibernate: o nome gerado pelo Hibernate é
-- determinístico por base, mas imprevisível entre ambientes. Por isso
-- localizamos via INFORMATION_SCHEMA.STATISTICS: procuramos um índice
-- NON_UNIQUE=0 cuja ÚNICA coluna seja a alvo, e dropamos pelo nome encontrado.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Helper: DROPa o índice UNIQUE de coluna única na tabela/coluna informadas.
-- Seta @drop_sql com 'ALTER TABLE <t> DROP INDEX <nome>' ou 'DO 0' (no-op).
-- -----------------------------------------------------------------------------
-- Para cada tabela/coluna, montamos a consulta que encontra o nome do índice
-- UNIQUE (NON_UNIQUE=0) que contém exatamente uma coluna e cuja coluna é a
-- informada. Se houver mais de um candidato, pegamos o primeiro (LIMIT 1).

-- companies.cnpj
SET @uk_name = (
    SELECT s.INDEX_NAME
    FROM INFORMATION_SCHEMA.STATISTICS s
    WHERE s.TABLE_SCHEMA = DATABASE()
      AND s.TABLE_NAME = 'companies'
      AND s.COLUMN_NAME = 'cnpj'
      AND s.NON_UNIQUE = 0
      AND s.INDEX_NAME <> 'PRIMARY'
      AND s.INDEX_NAME NOT LIKE 'uk_companies_org_cnpj'
      AND (
          SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS s2
          WHERE s2.TABLE_SCHEMA = DATABASE()
            AND s2.TABLE_NAME = 'companies'
            AND s2.INDEX_NAME = s.INDEX_NAME
        ) = 1
    LIMIT 1
);
SET @sql = IF(@uk_name IS NOT NULL,
    CONCAT('ALTER TABLE companies DROP INDEX `', @uk_name, '`'),
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- customers.cpf
SET @uk_name = (
    SELECT s.INDEX_NAME
    FROM INFORMATION_SCHEMA.STATISTICS s
    WHERE s.TABLE_SCHEMA = DATABASE()
      AND s.TABLE_NAME = 'customers'
      AND s.COLUMN_NAME = 'cpf'
      AND s.NON_UNIQUE = 0
      AND s.INDEX_NAME <> 'PRIMARY'
      AND s.INDEX_NAME NOT LIKE 'uk_customers_org_cpf'
      AND (
          SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS s2
          WHERE s2.TABLE_SCHEMA = DATABASE()
            AND s2.TABLE_NAME = 'customers'
            AND s2.INDEX_NAME = s.INDEX_NAME
        ) = 1
    LIMIT 1
);
SET @sql = IF(@uk_name IS NOT NULL,
    CONCAT('ALTER TABLE customers DROP INDEX `', @uk_name, '`'),
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sellers.cpf
SET @uk_name = (
    SELECT s.INDEX_NAME
    FROM INFORMATION_SCHEMA.STATISTICS s
    WHERE s.TABLE_SCHEMA = DATABASE()
      AND s.TABLE_NAME = 'sellers'
      AND s.COLUMN_NAME = 'cpf'
      AND s.NON_UNIQUE = 0
      AND s.INDEX_NAME <> 'PRIMARY'
      AND s.INDEX_NAME NOT LIKE 'uk_sellers_org_cpf'
      AND (
          SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS s2
          WHERE s2.TABLE_SCHEMA = DATABASE()
            AND s2.TABLE_NAME = 'sellers'
            AND s2.INDEX_NAME = s.INDEX_NAME
        ) = 1
    LIMIT 1
);
SET @sql = IF(@uk_name IS NOT NULL,
    CONCAT('ALTER TABLE sellers DROP INDEX `', @uk_name, '`'),
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- suppliers.tax_id
SET @uk_name = (
    SELECT s.INDEX_NAME
    FROM INFORMATION_SCHEMA.STATISTICS s
    WHERE s.TABLE_SCHEMA = DATABASE()
      AND s.TABLE_NAME = 'suppliers'
      AND s.COLUMN_NAME = 'tax_id'
      AND s.NON_UNIQUE = 0
      AND s.INDEX_NAME <> 'PRIMARY'
      AND s.INDEX_NAME NOT LIKE 'uk_suppliers_org_tax_id'
      AND (
          SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS s2
          WHERE s2.TABLE_SCHEMA = DATABASE()
            AND s2.TABLE_NAME = 'suppliers'
            AND s2.INDEX_NAME = s.INDEX_NAME
        ) = 1
    LIMIT 1
);
SET @sql = IF(@uk_name IS NOT NULL,
    CONCAT('ALTER TABLE suppliers DROP INDEX `', @uk_name, '`'),
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- 2. Cria as novas constraints UNIQUE COMPOSTAS (organization_uuid + coluna).
--    Idempotente: só cria se o índice ainda não existir.
-- -----------------------------------------------------------------------------

-- uk_companies_org_cnpj (organization_uuid, cnpj)
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'companies'
      AND INDEX_NAME = 'uk_companies_org_cnpj');
SET @sql = IF(@has_idx = 0,
    'CREATE UNIQUE INDEX uk_companies_org_cnpj ON companies (organization_uuid, cnpj)',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- uk_customers_org_cpf (organization_uuid, cpf)
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customers'
      AND INDEX_NAME = 'uk_customers_org_cpf');
SET @sql = IF(@has_idx = 0,
    'CREATE UNIQUE INDEX uk_customers_org_cpf ON customers (organization_uuid, cpf)',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- uk_sellers_org_cpf (organization_uuid, cpf)
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sellers'
      AND INDEX_NAME = 'uk_sellers_org_cpf');
SET @sql = IF(@has_idx = 0,
    'CREATE UNIQUE INDEX uk_sellers_org_cpf ON sellers (organization_uuid, cpf)',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- uk_suppliers_org_tax_id (organization_uuid, tax_id)
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'suppliers'
      AND INDEX_NAME = 'uk_suppliers_org_tax_id');
SET @sql = IF(@has_idx = 0,
    'CREATE UNIQUE INDEX uk_suppliers_org_tax_id ON suppliers (organization_uuid, tax_id)',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;