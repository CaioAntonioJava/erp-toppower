-- =============================================================================
-- V2__technical_proposal_service_item_description_text.sql
--
-- A descrição dos itens de serviço da proposta técnica passou a ser HTML
-- formatado (editado no frontend via RichTextEditor). O VARCHAR(2000) original
-- (NOT NULL) é insuficiente para conter as tags HTML, então a coluna passa
-- para TEXT e nullable — a descrição agora é opcional (uma linha de serviço
-- pode ser informada apenas pelo preço).
--
-- O Hibernate (ddl-auto=update) aplica a mudança de columnDefinition, mas
-- este script garante a alteração de forma idempotente em bases já existentes.
-- =============================================================================

ALTER TABLE technical_proposal_service_items
    MODIFY COLUMN description TEXT NULL;