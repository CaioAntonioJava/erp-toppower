-- =============================================================================
-- V10__cash_flow_settings.sql
--
-- Cria a tabela cash_flow_settings: configuração do fluxo de caixa por
-- organização, com o saldo inicial do controle de caixa (caixa existente
-- antes do primeiro movimento registrado no sistema). Um único registro
-- por organização (unique em organization_id).
--
-- Idempotente: spring.sql.init.mode=always roda a cada boot. O CREATE
-- TABLE IF NOT EXISTS evita erro se a tabela já foi criada pelo
-- ddl-auto=update; o guard via INFORMATION_SCHEMA adiciona a unique
-- constraint em instalações em que a tabela já existia sem ela.
-- =============================================================================

CREATE TABLE IF NOT EXISTS cash_flow_settings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    organization_id BIGINT NULL,
    opening_balance DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    created_by VARCHAR(255) NULL,
    updated_by VARCHAR(255) NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @has_idx = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'cash_flow_settings'
      AND INDEX_NAME = 'uk_cashflow_settings_org');
SET @sql = IF(@has_idx = 0,
    'ALTER TABLE cash_flow_settings ADD CONSTRAINT uk_cashflow_settings_org UNIQUE (organization_id)',
    'DO 0'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;