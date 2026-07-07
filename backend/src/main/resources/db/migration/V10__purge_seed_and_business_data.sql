-- =============================================================================
-- V10__purge_seed_and_business_data.sql
--
-- Zera TODAS as tabelas de negócio do ERP, removendo:
--   * Os registros seed órfãos herdados da antiga V5__seed_initial_data.sql
--     (UUIDs prefixados 00000000-0000-4000-8000/9000 e created_by =
--     'seed@toppower.local'), que permaneciam no banco porque a V5 era
--     idempotente apenas na inserção (nunca removia o que já tinha inserido).
--   * Eventuais registros reais cadastrados manualmente em dev.
--
-- ESCOPO — tabelas de negócio (14):
--   companies, customers, sellers, products, suppliers,
--   quotations, quotation_items,
--   sales_orders, sales_order_items,
--   stock_movements,
--   technical_proposals, technical_proposal_objectives,
--   technical_proposal_product_items, technical_proposal_service_items.
--
-- FORA DO ESCOPO (PRESERVADAS):
--   * ceps       — base local de ~850k CEPs, cara de recarregar.
--   * users      — contas de login; o BootstrapRunner recria o admin
--                  default apenas se a tabela estiver vazia.
--   * profiles   — perfil do usuário logado.
--
-- MOTIVO:
--   Limpeza da "base de mocks". A camada de mocks do frontend já havia sido
--   removida (commit 9eb8f2f) e a migration V5 (que reproduzia esses mocks no
--   banco) foi excluída. Esta V10 garante que bancos já populados por boots
--   anteriores fiquem limpos, sem deixar órfãos.
--
-- IDEMPOTÊNCIA:
--   Executada a cada boot (spring.sql.init.mode=always). DELETE é
--   naturalmente idempotente: em boots subsequentes as tabelas já estarão
--   vazias e o comando é no-op. Nenhum dado de schema é tocado.
--
-- ORDEM:
--   Itens (filhos) antes dos cabeçalhos (pais) para evitar referências
--   lógicas quebradas — as FKs são lógicas (não físicas), mas a ordem
--   mantém consistência para consultas e relatórios.
-- =============================================================================

-- =============================================================================
-- 1) Itens / detalhes (filhos) — limpar primeiro
-- =============================================================================

-- Estoque (diário de movimentações)
DELETE FROM stock_movements;

-- Itens de pedido de venda
DELETE FROM sales_order_items;

-- Itens de proposta técnica (objetivos, produtos, serviços)
DELETE FROM technical_proposal_objectives;
DELETE FROM technical_proposal_product_items;
DELETE FROM technical_proposal_service_items;

-- Itens de cotação/proposta comercial
DELETE FROM quotation_items;

-- =============================================================================
-- 2) Cabeçalhos / cadastros (pais)
-- =============================================================================

-- Pedidos de venda
DELETE FROM sales_orders;

-- Propostas técnicas
DELETE FROM technical_proposals;

-- Cotações/propostas comerciais
DELETE FROM quotations;

-- Cadastros base
DELETE FROM companies;
DELETE FROM customers;
DELETE FROM sellers;
DELETE FROM products;
DELETE FROM suppliers;