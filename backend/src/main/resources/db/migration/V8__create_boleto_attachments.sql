-- Tabela de anexos de boletos (PDF/imagens). Cada boleto pode ter N anexos.
-- Segue a convenção do projeto: PK BIGINT AUTO_INCREMENT, sem FKs físicas,
-- coluna organization_id para isolamento multi-tenant, auditoria criada
-- pelo Hibernate (ddl-auto=update). O arquivo é persistido em disco sob
-- <app.uploads.dir>/attachments/boletos/; esta tabela guarda metadados + URL.
CREATE TABLE IF NOT EXISTS boleto_attachments (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    organization_id BIGINT       NULL,
    boleto_id       BIGINT       NOT NULL,
    file_name       VARCHAR(255) NOT NULL,
    stored_name     VARCHAR(255) NOT NULL,
    content_type    VARCHAR(100) NOT NULL,
    size_bytes      BIGINT       NOT NULL,
    public_url      VARCHAR(255) NOT NULL,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    created_by      VARCHAR(100) NULL,
    updated_by      VARCHAR(100) NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Índice para listar anexos de um boleto.
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'boleto_attachments' AND INDEX_NAME = 'idx_boleto_attachments_boleto_id');
SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_boleto_attachments_boleto_id ON boleto_attachments (boleto_id)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Índice para escopar por organização.
SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'boleto_attachments' AND INDEX_NAME = 'idx_boleto_attachments_organization_id');
SET @sql = IF(@has_idx = 0, 'CREATE INDEX idx_boleto_attachments_organization_id ON boleto_attachments (organization_id)', 'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;