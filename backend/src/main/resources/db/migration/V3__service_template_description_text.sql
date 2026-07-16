-- =============================================================================
-- V3__service_template_description_text.sql
--
-- A descrição do template de serviço passou a ser HTML formatado (editado no
-- frontend via RichTextEditor). O VARCHAR(255) padrão gerado pelo Hibernate
-- (ddl-auto=update) é insuficiente para conter as tags HTML, então a coluna
-- passa para TEXT e nullable — a descrição é opcional.
--
-- O Hibernate (ddl-auto=update) com columnDefinition = "TEXT" na entidade
-- não altera colunas já existentes. Este script garante a alteração de forma
-- idempotente em bases já existentes.
-- =============================================================================

ALTER TABLE service_templates
    MODIFY COLUMN description TEXT NULL;
