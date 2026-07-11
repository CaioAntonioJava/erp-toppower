-- =============================================================================
-- V30__add_company_to_contracts.sql
--
-- Adiciona a coluna `company_uuid` na tabela `contracts`, permitindo que
-- um contrato seja vinculado a uma empresa (pessoa jurídica) além do
-- cliente pessoa física (`customer_uuid`) que já existia.
--
-- Contexto:
--   Até a V29, o módulo de contratos aceitava apenas cliente PF
--   (`customer_uuid` NOT NULL). A partir desta migration, o contrato
--   segue o mesmo padrão da Proposta Técnica: exatamente UM entre
--   `customer_uuid` (PF) e `company_uuid` (PJ) deve estar preenchido.
--   A invariante é garantida pelo `ContractService` no momento da
--   criação/atualização, não por constraint SQL (a coluna PF é
--   nullable agora para permitir PJ puro).
--
--   A coluna `customer_uuid` é mantida (mas passa a ser nullable) para
--   preservar contratos existentes. A migration não tenta popular a
--   coluna para linhas existentes — contratos já gravados com PF
--   permanecem com o mesmo cliente.
--
-- Idempotência:
--   Padrão PREPARE/EXECUTE dinâmico idêntico ao de V18/V19/V22/V25/V29,
--   pois `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` e `CREATE INDEX
--   IF NOT EXISTS` não são suportados na versão do MySQL usada neste
--   projeto.
-- =============================================================================

-- 1. Adiciona `company_uuid` (nullable) à tabela `contracts`
SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'contracts'
      AND COLUMN_NAME = 'company_uuid');
SET @sql = IF(@has_col = 0,
    'ALTER TABLE contracts ADD COLUMN company_uuid BINARY(16) NULL AFTER customer_uuid',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. Torna `customer_uuid` nullable (para permitir contratos PJ puros).
--    Idempotente: checa IS_NULLABLE antes de aplicar MODIFY COLUMN.
SET @is_nullable = (SELECT IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'contracts'
      AND COLUMN_NAME = 'customer_uuid');
SET @sql = IF(@is_nullable = 'NO',
    'ALTER TABLE contracts MODIFY COLUMN customer_uuid BINARY(16) NULL',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3. Índice em `company_uuid` (para queries/joins por PJ).
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'contracts'
      AND INDEX_NAME = 'idx_contract_company');
SET @sql = IF(@has_idx = 0,
    'CREATE INDEX idx_contract_company ON contracts (company_uuid)',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;