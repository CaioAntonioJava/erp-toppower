-- ============================================================================
-- Migration V36: Adiciona a coluna delivery_deadline na tabela contracts
-- ============================================================================
-- O módulo de Contratos passou a suportar um campo opcional de texto livre
-- "Prazo de entrega", exibido no formulário abaixo da seção "Cláusula 2"
-- e antes de "Valor total", e replicado no detalhe e no PDF.
--
-- Esta coluna é puramente textual (VARCHAR(500)), nullable e não tem
-- semântica de data — o usuário digita livremente (ex.: "30 dias úteis",
-- "15 dias após a assinatura", "entrega imediata"). Contratos antigos
-- permanecem válidos com delivery_deadline = NULL.
-- ============================================================================

ALTER TABLE contracts
    ADD COLUMN delivery_deadline VARCHAR(500) NULL AFTER products_description;