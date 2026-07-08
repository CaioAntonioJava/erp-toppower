-- =============================================================================
-- V25__add_organization_proposal_prefix_and_scope_tp_code.sql
--
-- 1) Adiciona a coluna `proposal_prefix` na tabela `organizations`, que define
--    o prefixo do código das Propostas Técnicas emitidas por cada empresa
--    (PT-001-2026 para Top Power Engenharia, PL-001-2026 para Top Power
--    Materiais). A coluna é NOT NULL após a migração e tem UNIQUE global
--    (cada prefixo pertence a uma única empresa).
--
-- 2) Popula a coluna para as duas Organizations Top Power (idempotente).
--
-- 3) Reescreve a UNIQUE KEY de `technical_proposals`: antes era global
--    (prefix, sequence, year); agora passa a ser escopada por Organization
--    (organization_uuid, prefix, sequence, year), para que cada empresa
--    tenha sua própria sequência e possa reiniciar a contagem
--    independentemente a cada ano.
--
-- Idempotência:
--   Padrão PREPARE/EXECUTE dinâmico idêntico ao de V18/V22/V24, pois
--   `ALTER TABLE ... ADD COLUMN IF NOT EXISTS`, `CREATE INDEX IF NOT
--   EXISTS` e `DROP INDEX IF EXISTS` não são suportados no MySQL
--   utilizado neste projeto.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. Coluna `proposal_prefix` em `organizations`
-- -----------------------------------------------------------------------------

-- Adiciona a coluna (nullable primeiro para tolerar bases pré-existentes
-- sem o valor; o backfill abaixo popula todas as orgs Top Power; a
-- alteração para NOT NULL é feita após o backfill).
SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'organizations'
      AND COLUMN_NAME = 'proposal_prefix');
SET @sql = IF(@has_col = 0,
    'ALTER TABLE organizations ADD COLUMN proposal_prefix VARCHAR(10) NULL AFTER state',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3. Backfill: popula a coluna para as duas orgs Top Power.
--    Idempotente: as condições IS NULL OR = '' cobrem tanto bases
--    recém-criadas (coluna NULL) quanto bases onde o Hibernate
--    ddl-auto=update criou a coluna com string vazia em boots
--    anteriores.
UPDATE organizations SET proposal_prefix = 'PT'
WHERE cnpj = '13.433.616/0001-06' AND (proposal_prefix IS NULL OR proposal_prefix = '');

UPDATE organizations SET proposal_prefix = 'PL'
WHERE cnpj = '59.530.698/0001-08' AND (proposal_prefix IS NULL OR proposal_prefix = '');

-- 3b. Fallback de segurança: se a coluna foi criada via Hibernate
--     ddl-auto=update (sem o NOT NULL e sem o backfill deste script) em
--     boots anteriores, podem existir rows com proposal_prefix NULL ou
--     string vazia. Para garantir que o UNIQUE INDEX abaixo não falhe
--     em bases pré-existentes (duplicatas de '' ou NULL são tratadas
--     como iguais pelo MySQL no UNIQUE), preenchemos qualquer valor
--     nulo/vazio com 'PL' como fallback seguro. O backfill por CNPJ
--     acima (passo 3) já terá sobrescrito as orgs Top Power com PT/PL.
UPDATE organizations SET proposal_prefix = 'PL'
WHERE proposal_prefix IS NULL OR proposal_prefix = '';

-- 2. UNIQUE INDEX uk_organizations_proposal_prefix em organizations
--    Idempotente: só cria se o índice ainda não existir.
--    IMPORTANTE: este índice é criado DEPOIS do backfill (passos 3 e 3b)
--    para evitar a violação "Duplicate entry '' for key
--    uk_organizations_proposal_prefix" em bases onde o Hibernate já
--    tenha criado a coluna proposal_prefix (vazia/NULL) em boots
--    anteriores. O índice também é tolerante a execuções parciais
--    anteriores que tenham chegado a criá-lo: a checagem por
--    INFORMATION_SCHEMA garante idempotência.
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'organizations'
      AND INDEX_NAME = 'uk_organizations_proposal_prefix');
SET @sql = IF(@has_idx = 0,
    'CREATE UNIQUE INDEX uk_organizations_proposal_prefix ON organizations (proposal_prefix)',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4. Após o backfill, força NOT NULL. Como a coluna pode ter sido criada
--    com nullable em uma execução anterior, checamos se já é NOT NULL.
SET @is_nullable = (SELECT IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'organizations'
      AND COLUMN_NAME = 'proposal_prefix');
SET @sql = IF(@is_nullable = 'YES',
    'ALTER TABLE organizations MODIFY COLUMN proposal_prefix VARCHAR(10) NOT NULL',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- 5. Reescrita da UNIQUE KEY de `technical_proposals`
--    - Dropa a UK antiga (uk_technical_proposal_code) que era
--      (prefix, sequence, year) — localizada dinamicamente por ter
--      exatamente 3 colunas e NON_UNIQUE=0.
--    - Cria a nova UK escopada por Organization
--      (uk_technical_proposal_org_code) com 4 colunas
--      (organization_uuid, prefix, sequence, year).
-- -----------------------------------------------------------------------------

-- 5a. Dropa uk_technical_proposal_code (3 colunas, NON_UNIQUE=0, != PRIMARY)
SET @uk_name = (
    SELECT s.INDEX_NAME
    FROM INFORMATION_SCHEMA.STATISTICS s
    WHERE s.TABLE_SCHEMA = DATABASE()
      AND s.TABLE_NAME = 'technical_proposals'
      AND s.NON_UNIQUE = 0
      AND s.INDEX_NAME <> 'PRIMARY'
      AND s.INDEX_NAME <> 'uk_technical_proposal_org_code'
      AND s.INDEX_NAME NOT LIKE 'uk_technical_proposal_org_code%'
      AND s.COLUMN_NAME IN ('prefix', 'sequence', 'year')
      AND (
          SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS s2
          WHERE s2.TABLE_SCHEMA = DATABASE()
            AND s2.TABLE_NAME = 'technical_proposals'
            AND s2.INDEX_NAME = s.INDEX_NAME
        ) = 3
    LIMIT 1
);
SET @sql = IF(@uk_name IS NOT NULL,
    CONCAT('ALTER TABLE technical_proposals DROP INDEX `', @uk_name, '`'),
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 5b. Cria uk_technical_proposal_org_code (organization_uuid, prefix, sequence, year)
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'technical_proposals'
      AND INDEX_NAME = 'uk_technical_proposal_org_code');
SET @sql = IF(@has_idx = 0,
    'CREATE UNIQUE INDEX uk_technical_proposal_org_code ON technical_proposals (organization_uuid, prefix, sequence, year)',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
