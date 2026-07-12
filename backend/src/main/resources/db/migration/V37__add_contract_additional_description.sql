-- ============================================================================
-- Migration V37: Adiciona a coluna additional_description na tabela contracts
-- ============================================================================
-- O módulo de Contratos passou a suportar um bloco de texto opcional
-- "Descrição adicional", exibido no formulário abaixo da seção "Cláusula 3"
-- e antes de "Valor total", e replicado na página de detalhe e no PDF.
--
-- Esta coluna é puramente textual (TEXT), nullable e armazena HTML rico
-- (mesma semântica de `description`, `services_description` e
-- `products_description`). Contratos antigos permanecem válidos com
-- additional_description = NULL — a seção simplesmente não é renderizada.
-- ============================================================================

ALTER TABLE contracts
    ADD COLUMN additional_description TEXT NULL AFTER delivery_deadline;