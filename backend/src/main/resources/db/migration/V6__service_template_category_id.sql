-- =============================================================================
-- V6__service_template_category_id.sql
--
-- Migração para usar a tabela `categories` (já existente) como domínio de
-- categorias dos `service_templates`, referenciada por `category_id`.
--
-- A tabela `categories` já existe no banco (criada anteriormente). A coluna
-- `status` (VARCHAR 20) é adicionada pelo Hibernate (ddl-auto=update) a
-- partir da entidade ServiceCategory. A coluna `category_id` em
-- `service_templates` também é gerenciada pelo Hibernate.
--
-- Esta migration garante apenas:
--   1) A coluna `category_id` em `service_templates` (se Hibernate ainda
--      não criou) — nullable inicialmente, tornada NOT NULL no final.
--   2) Backfill de `category_id` usando a join table `service_template_categories`
--      (dados já mapeados), para o caso de haver registros.
--   3) Torna `category_id` NOT NULL (após o backfill).
--   4) Garante índice unique em `categories.name`.
--
-- Nota: a coluna VARCHAR `category` em `service_templates` foi removida
-- quando o campo foi retirado da entidade. Esta migration NÃO a referencia.
--
-- Idempotente: spring.sql.init.mode=always roda a cada boot. Usa guards
-- via INFORMATION_SCHEMA (MySQL 8+) e PREPARE/EXECUTE.
-- =============================================================================

-- 1) Adiciona a coluna category_id em service_templates (nullable)
--    O Hibernate (ddl-auto=update) normalmente já cria esta coluna, mas
--    garantimos aqui para o caso de rodar antes do Hibernate.
SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'service_templates'
      AND COLUMN_NAME = 'category_id');
SET @sql = IF(@has_col = 0,
    'ALTER TABLE service_templates ADD COLUMN category_id BIGINT NULL',
    'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) Backfill: preenche category_id a partir da join table
--    service_template_categories (dados já mapeados manualmente).
--    Só executa se houver registros na join table.
--    Guard extra: em banco recém-criado a join table legada
--    `service_template_categories` não existe (não há mais entidade JPA
--    para ela, logo o Hibernate não a cria). Nesse caso não há dados a
--    migrar e o backfill é simplesmente pulado.
SET @has_join_table = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'service_template_categories');
SET @sql = IF(@has_join_table > 0,
    'UPDATE service_templates st JOIN ( SELECT service_template_id, MIN(category_id) AS cat_id FROM service_template_categories GROUP BY service_template_id ) stc ON st.id = stc.service_template_id SET st.category_id = stc.cat_id WHERE st.category_id IS NULL',
    'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3) Torna category_id NOT NULL (após o backfill)
SET @is_nullable = (SELECT IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'service_templates'
      AND COLUMN_NAME = 'category_id');
SET @sql = IF(@is_nullable = 'NO',
    'DO 0',
    'ALTER TABLE service_templates MODIFY COLUMN category_id BIGINT NOT NULL');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4) Garante índice unique em categories.name.
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'categories'
      AND INDEX_NAME = 'uk_categories_name');
SET @sql = IF(@has_idx = 0,
    'CREATE UNIQUE INDEX uk_categories_name ON categories (name)',
    'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;