-- =============================================================================
-- V18__migrate_product_unit_to_sefaz.sql
--
-- Converte os valores legados da coluna products.unit_type (nomes em português
-- usados antes da refatoração) para os códigos oficiais da NF-e (tabela SEFAZ).
--
-- Mapeamento:
--   UNIDADE -> UN
--   METROS  -> MTR
--   BOBINA  -> RL  (na tabela SEFAZ, "rolo")
--   PECAS   -> PC
--   QUILOS  -> KG
--   ROLO    -> RL
--
-- Passo 1 (ALTER): garante que a coluna seja VARCHAR(20), independentemente
-- do tipo anterior. Em bases legadas a coluna pode ter sido criada como
-- ENUM('UNIDADE','METROS',...) (não pelo Hibernate atual, que mapeia
-- @Enumerated(EnumType.STRING) para VARCHAR); o ddl-auto=update NÃO converte
-- ENUM -> VARCHAR, então forçamos aqui. A conversão preserva os valores
-- string existentes (os nomes dos membros do ENUM viram texto VARCHAR).
-- Sem este passo, os UPDATEs abaixo falham com "Data truncated" ao tentar
-- gravar 'UN' (não-membro do ENUM antigo).
--
-- Passo 2 (UPDATEs): normaliza os valores legados. Idempotente — cada UPDATE
-- só afeta linhas com o valor legado; rodar mais de uma vez é no-op.
--
-- Roda em todo boot (spring.sql.init.mode=always), por isso é idempotente.
-- =============================================================================

-- Passo 1: força o tipo VARCHAR(20) NOT NULL (mesmo que já seja esse).
ALTER TABLE products MODIFY COLUMN unit_type VARCHAR(20) NOT NULL;

-- Passo 2: normaliza valores legados -> códigos SEFAZ.
UPDATE products SET unit_type = 'UN'  WHERE unit_type = 'UNIDADE';
UPDATE products SET unit_type = 'MTR' WHERE unit_type = 'METROS';
UPDATE products SET unit_type = 'RL'  WHERE unit_type = 'BOBINA';
UPDATE products SET unit_type = 'PC'  WHERE unit_type = 'PECAS';
UPDATE products SET unit_type = 'KG'  WHERE unit_type = 'QUILOS';
UPDATE products SET unit_type = 'RL'  WHERE unit_type = 'ROLO';