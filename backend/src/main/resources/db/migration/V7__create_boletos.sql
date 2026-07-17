-- Tabela de boletos (cadastro de boletos do sistema).
-- Siga a convenção do projeto: PK BIGINT AUTO_INCREMENT, sem FKs físicas,
-- coluna organization_id para isolamento multi-tenant (mesmo sem constraint
-- física, o OrganizationEntityListener preenche no persist e o
-- OrganizationFilterAspect escopa queries JPQL/Criteria).
-- O Hibernate (ddl-auto=update) criará as colunas de auditoria (created_at,
-- updated_at, created_by, updated_by) e organization_id; este script garante
-- apenas a tabela, os índices e a unicidade por organização.
CREATE TABLE IF NOT EXISTS boletos (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    organization_id BIGINT       NULL,
    document_number VARCHAR(50)  NOT NULL,
    payee           VARCHAR(200) NOT NULL,
    value           DECIMAL(12, 2) NOT NULL,
    due_date        DATE         NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ATIVO',
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    created_by      VARCHAR(100) NULL,
    updated_by      VARCHAR(100) NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Índice auxiliar para filtrar boletos ativos/inativos.
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'boletos' AND INDEX_NAME = 'idx_boletos_status');
SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_boletos_status ON boletos (status)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Índice auxiliar para a busca por número do documento.
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'boletos' AND INDEX_NAME = 'idx_boletos_document_number');
SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_boletos_document_number ON boletos (document_number)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Índice auxiliar para ordenação por vencimento.
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'boletos' AND INDEX_NAME = 'idx_boletos_due_date');
SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_boletos_due_date ON boletos (due_date)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Índice para escopar por organização (usado pelas queries JPQL escopadas).
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'boletos' AND INDEX_NAME = 'idx_boletos_organization_id');
SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_boletos_organization_id ON boletos (organization_id)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Unicidade do número do documento por organização (boletos ativos/inativos
-- compartilham o mesmo namespace dentro da organização).
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'boletos' AND INDEX_NAME = 'uk_boletos_org_document_number');
SET @sql = IF(@has_idx = 0, 'CREATE UNIQUE INDEX uk_boletos_org_document_number ON boletos (organization_id, document_number)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;