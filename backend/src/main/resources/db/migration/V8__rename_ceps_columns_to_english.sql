-- =============================================================================
-- V8__rename_ceps_columns_to_english.sql
--
-- Renomeia as colunas da tabela `ceps` de PT-BR para ingles, alinhando o
-- schema persistido aos identificadores da entidade Cep (refatorada para
-- street/neighborhood/city/state).
--
-- Contexto:
--   O modulo `cep` foi refatorado para seguir o padrao ingles do restante
--   do backend. A entidade Cep agora declara @Column(name="street"),
--   "neighborhood", "city", "state". Em DBs ja existentes, o Hibernate
--   (ddl-auto=update) cria as novas colunas nullable ao lado das antigas
--   (logradouro, bairro, cidade, uf) — esta migration copia os dados das
--   colunas antigas para as novas, remove as antigas e recria o indice.
--
--   Em DB fresh (sem colunas antigas), todos os blocos sao guardados por
--   checagem em INFORMATION_SCHEMA e nada e executado — a tabela ja nasce
--   com as colunas em ingles.
--
-- Idempotente:
--   Roda em todo boot (spring.sql.init.mode=always). Cada bloco so executa
--   se a coluna/indice antigo ainda existir. Apos a primeira execucao em
--   um DB legado, os guards impedem nova execucao.
--
-- Nota:
--   `ALTER TABLE ... DROP COLUMN IF EXISTS` nao e suportado por todas as
--   versoes do MySQL (apenas 8.0.29+); por isso todos os DROPs usam
--   PREPARE/EXECUTE dinâmico com guard em INFORMATION_SCHEMA.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. Copiar dados das colunas antigas (PT-BR) para as novas (EN), quando as
--    antigas ainda existem. Em DB fresh, nada acontece.
-- -----------------------------------------------------------------------------

-- logradouro -> street
SET @has_logradouro = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ceps'
      AND COLUMN_NAME = 'logradouro'
);
SET @sql = IF(@has_logradouro = 1,
    'UPDATE ceps SET street = COALESCE(street, logradouro)',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- bairro -> neighborhood
SET @has_bairro = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ceps'
      AND COLUMN_NAME = 'bairro'
);
SET @sql = IF(@has_bairro = 1,
    'UPDATE ceps SET neighborhood = COALESCE(neighborhood, bairro)',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- cidade -> city
SET @has_cidade = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ceps'
      AND COLUMN_NAME = 'cidade'
);
SET @sql = IF(@has_cidade = 1,
    'UPDATE ceps SET city = COALESCE(city, cidade)',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- uf -> state
SET @has_uf = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ceps'
      AND COLUMN_NAME = 'uf'
);
SET @sql = IF(@has_uf = 1,
    'UPDATE ceps SET state = COALESCE(state, uf)',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- 2. DROP das colunas antigas. A PK natural da tabela e `cep` (acrônimo)
--    e nao e tocada. Cada DROP e guardado pela existencia da coluna.
-- -----------------------------------------------------------------------------

SET @sql = IF(@has_logradouro = 1,
    'ALTER TABLE ceps DROP COLUMN logradouro',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(@has_bairro = 1,
    'ALTER TABLE ceps DROP COLUMN bairro',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(@has_cidade = 1,
    'ALTER TABLE ceps DROP COLUMN cidade',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(@has_uf = 1,
    'ALTER TABLE ceps DROP COLUMN uf',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- 3. Recriar o indice por estado/cidade. O indice antigo so e removido se
--    ainda existir; o novo so e criado se ainda nao existir.
-- -----------------------------------------------------------------------------

SET @has_old_idx = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ceps'
      AND INDEX_NAME = 'idx_ceps_uf_cidade'
);
SET @sql = IF(@has_old_idx = 1,
    'ALTER TABLE ceps DROP INDEX idx_ceps_uf_cidade',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_new_idx = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ceps'
      AND INDEX_NAME = 'idx_ceps_state_city'
);
SET @sql = IF(@has_new_idx = 0,
    'CREATE INDEX idx_ceps_state_city ON ceps (state, city)',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;