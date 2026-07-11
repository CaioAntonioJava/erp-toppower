-- =============================================================================
-- V29__add_organization_contract_prefix.sql
--
-- Adiciona a coluna `contract_prefix` na tabela `organizations`, que define o
-- prefixo do código dos Contratos emitidos por cada empresa
-- (CT-001-2026 para Top Power Engenharia, CL-001-2026 para Top Power
-- Materiais). A coluna é NOT NULL após a migração e tem UNIQUE global
-- (cada prefixo pertence a uma única empresa).
--
-- Idempotência:
--   Padrão PREPARE/EXECUTE dinâmico idêntico ao de V25 (proposal_prefix),
--   pois `ALTER TABLE ... ADD COLUMN IF NOT EXISTS`, `CREATE INDEX IF NOT
--   EXISTS` e `DROP INDEX IF EXISTS` não são suportados no MySQL utilizado
--   neste projeto.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. Coluna `contract_prefix` em `organizations`
-- -----------------------------------------------------------------------------

-- Adiciona a coluna (nullable primeiro para tolerar bases pré-existentes
-- sem o valor; o backfill abaixo popula todas as orgs Top Power; a alteração
-- para NOT NULL é feita após o backfill).
SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'organizations'
      AND COLUMN_NAME = 'contract_prefix');
SET @sql = IF(@has_col = 0,
    'ALTER TABLE organizations ADD COLUMN contract_prefix VARCHAR(10) NULL AFTER proposal_prefix',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. Backfill: popula a coluna para as duas orgs Top Power.
--    Idempotente: as condições IS NULL OR = '' cobrem tanto bases
--    recém-criadas (coluna NULL) quanto bases onde o Hibernate
--    ddl-auto=update criou a coluna com string vazia em boots
--    anteriores.
UPDATE organizations SET contract_prefix = 'CT'
WHERE cnpj = '13.433.616/0001-06' AND (contract_prefix IS NULL OR contract_prefix = '');

UPDATE organizations SET contract_prefix = 'CL'
WHERE cnpj = '59.530.698/0001-08' AND (contract_prefix IS NULL OR contract_prefix = '');

-- 2b. Fallback de segurança: se a coluna foi criada via Hibernate
--     ddl-auto=update (sem o NOT NULL e sem o backfill deste script) em
--     boots anteriores, podem existir rows com contract_prefix NULL ou
--     string vazia. Para garantir que o UNIQUE INDEX abaixo não falhe
--     em bases pré-existentes (duplicatas de '' ou NULL são tratadas
--     como iguais pelo MySQL no UNIQUE), preenchemos qualquer valor
--     nulo/vazio com 'CL' como fallback seguro. O backfill por CNPJ
--     acima (passo 2) já terá sobrescrito as orgs Top Power com CT/CL.
UPDATE organizations SET contract_prefix = 'CL'
WHERE contract_prefix IS NULL OR contract_prefix = '';

-- 3. UNIQUE INDEX uk_organizations_contract_prefix em organizations
--    Idempotente: só cria se o índice ainda não existir.
--    Criado DEPOIS do backfill para evitar violações em bases onde o
--    Hibernate já tenha criado a coluna (vazia/NULL) em boots anteriores.
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'organizations'
      AND INDEX_NAME = 'uk_organizations_contract_prefix');
SET @sql = IF(@has_idx = 0,
    'CREATE UNIQUE INDEX uk_organizations_contract_prefix ON organizations (contract_prefix)',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4. Após o backfill, força NOT NULL. Como a coluna pode ter sido criada
--    com nullable em uma execução anterior, checamos se já é NOT NULL.
SET @is_nullable = (SELECT IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'organizations'
      AND COLUMN_NAME = 'contract_prefix');
SET @sql = IF(@is_nullable = 'YES',
    'ALTER TABLE organizations MODIFY COLUMN contract_prefix VARCHAR(10) NOT NULL',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;