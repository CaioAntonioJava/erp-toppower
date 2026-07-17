-- =============================================================================
-- V12__create_contract_clauses.sql
--
-- Cria a tabela contract_clauses, que armazena as cláusulas de um contrato
-- de prestação de serviços. Cada cláusula pertence a um contrato (contract_id)
-- e é isolada por organização (organization_id), seguindo a convenção do
-- projeto de referência por ID (sem FK física).
--
-- A cláusula de número 1 (DO OBJETO) pode referenciar um ServiceTemplate
-- (service_template_id) cuja descrição foi copiada para o content no
-- momento da criação do contrato.
--
-- Idempotente: CREATE TABLE IF NOT EXISTS.
-- =============================================================================

CREATE TABLE IF NOT EXISTS contract_clauses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    contract_id BIGINT NOT NULL,
    clause_number INT NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    service_template_id BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    INDEX idx_contract_clause_contract (contract_id),
    INDEX idx_contract_clause_org (organization_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;