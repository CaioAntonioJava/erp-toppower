-- ============================================================================
-- Migration V34: Cria a tabela contract_clauses e migra dados existentes
-- ============================================================================
-- A feature de cláusula única (coluna clause na tabela contracts) foi
-- substituída por uma lista dinâmica de cláusulas, cada uma em uma linha
-- da nova tabela contract_clauses.
--
-- Esta migration:
--   1. Cria a tabela contract_clauses
--   2. Migra os dados existentes da coluna clause para a nova tabela
--   3. Remove a coluna clause da tabela contracts
-- ============================================================================

CREATE TABLE contract_clauses (
    uuid BINARY(16) NOT NULL,
    organization_uuid BINARY(16) NOT NULL,
    contract_uuid BINARY(16) NOT NULL,
    description VARCHAR(4000) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    PRIMARY KEY (uuid),
    INDEX idx_cc_contract (contract_uuid),
    CONSTRAINT fk_cc_contract FOREIGN KEY (contract_uuid) REFERENCES contracts(uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Migrar dados existentes da coluna clause para a nova tabela
INSERT INTO contract_clauses (uuid, organization_uuid, contract_uuid, description, created_at, updated_at, created_by, updated_by)
SELECT UUID(), c.organization_uuid, c.uuid, c.clause, c.created_at, c.updated_at, c.created_by, c.updated_by
FROM contracts c
WHERE c.clause IS NOT NULL AND c.clause != '';

-- Remover coluna antiga
ALTER TABLE contracts DROP COLUMN clause;
