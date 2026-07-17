-- Tabela de contratos (cadastro de contratos de prestação de serviços).
-- Siga a convenção do projeto: PK BIGINT AUTO_INCREMENT, sem FKs físicas,
-- coluna organization_id para isolamento multi-tenant (o
-- OrganizationEntityListener preenche no persist e o
-- OrganizationFilterAspect escopa queries JPQL/Criteria).
-- O Hibernate (ddl-auto=update) criará as colunas de auditoria (created_at,
-- updated_at, created_by, updated_by) e organization_id; este script garante
-- apenas a tabela, os índices e a unicidade do código comercial por
-- organização/ano.
CREATE TABLE IF NOT EXISTS contracts (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    organization_id BIGINT       NULL,
    prefix          VARCHAR(10)  NOT NULL,
    sequence        BIGINT       NOT NULL,
    year            INT          NOT NULL,
    title           VARCHAR(300) NOT NULL,
    description     TEXT         NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ATIVO',
    issue_date      DATE         NOT NULL,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    created_by      VARCHAR(100) NULL,
    updated_by      VARCHAR(100) NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Índice auxiliar para filtrar contratos ativos/inativos.
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'contracts' AND INDEX_NAME = 'idx_contract_status');
SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_contract_status ON contracts (status)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Índice auxiliar para ordenação por data de emissão.
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'contracts' AND INDEX_NAME = 'idx_contract_issue_date');
SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_contract_issue_date ON contracts (issue_date)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Índice para escopar por organização (usado pelas queries JPQL escopadas).
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'contracts' AND INDEX_NAME = 'idx_contract_organization_id');
SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_contract_organization_id ON contracts (organization_id)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Unicidade do código comercial por organização/ano: a sequência
-- (prefix + sequence + year) é independente por Organization, não podendo
-- haver dois contratos com a mesma trinca dentro da mesma empresa.
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'contracts' AND INDEX_NAME = 'uk_contract_org_code');
SET @sql = IF(@has_idx = 0, 'CREATE UNIQUE INDEX uk_contract_org_code ON contracts (organization_id, prefix, sequence, year)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;