-- Migration V3: Quotation number passa de VARCHAR(50) (formato QUO001500) para BIGINT.
--
-- Contexto:
--   A pedido do time de produto, o número da proposta deixa de ter prefixo
--   literal e passa a ser um valor numérico puro. A numeração é gerada
--   pela aplicação (iniciando em 1500 e incrementando em +1 por proposta),
--   e a coluna agora é BIGINT, alinhada com a semântica do campo Java
--   (Long).
--
-- Importante:
--   O Hibernate com `ddl-auto=update` NÃO altera o tipo de uma coluna
--   existente, por isso esta migration é necessária.
--
--   Linhas pré-existentes (no formato antigo "QUO001500") são INCOMPATÍVEIS
--   com o novo tipo e, portanto, removidas antes do ALTER. Como o módulo
--   Quotation é novo e ainda não tem dados de produção, esta operação é
--   segura no contexto atual.
--
-- ATENÇÃO: este DROP + ALTER destrutivo. Em ambiente de produção com
-- dados, faça backup antes e/ou substitua este script por uma migration
-- que extraia o sufixo numérico e faça o backfill.

-- 1) Limpa as linhas (itens primeiro por causa da FK — embora aqui ainda
--    não exista FK formal, é a ordem correta caso seja adicionada).
DELETE FROM quotation_items;
DELETE FROM quotations;

-- 2) Altera a coluna `number` para BIGINT preservando NOT NULL e UNIQUE.
ALTER TABLE quotations MODIFY COLUMN number BIGINT NOT NULL;
