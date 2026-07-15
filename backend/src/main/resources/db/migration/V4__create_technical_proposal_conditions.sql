-- =============================================================================
-- V4__create_technical_proposal_conditions.sql
--
-- Cria a tabela de condições da proposta técnica. Cada condição é um item
-- com título + conteúdo (TEXT) + ordem de exibição, pertencente a uma
-- proposta técnica.
--
-- O Hibernate (ddl-auto=update) criaria a tabela a partir da entidade, mas
-- como o projeto usa migrations SQL manuais, este script garante a criação
-- de forma idempotente.
-- =============================================================================

CREATE TABLE IF NOT EXISTS technical_proposal_conditions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    technical_proposal_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    content TEXT,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    INDEX idx_tp_condition_proposal (technical_proposal_id),
    INDEX idx_tp_condition_org (organization_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
