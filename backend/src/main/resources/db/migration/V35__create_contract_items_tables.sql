-- ============================================================================
-- Migration V35: Cria tabelas de itens de serviço e produto para contratos
-- ============================================================================
-- Os contratos agora suportam itens estruturados de serviço (apenas descrição)
-- e produto (referência + quantidade), além dos campos de texto livre
-- services_description e products_description que já existiam.
--
-- Também adiciona o campo total_value (preenchimento manual) na tabela
-- contracts.
-- ============================================================================

CREATE TABLE contract_service_items (
    uuid BINARY(16) NOT NULL,
    organization_uuid BINARY(16) NOT NULL,
    contract_uuid BINARY(16) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    PRIMARY KEY (uuid),
    INDEX idx_csi_contract (contract_uuid),
    CONSTRAINT fk_csi_contract FOREIGN KEY (contract_uuid) REFERENCES contracts(uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE contract_product_items (
    uuid BINARY(16) NOT NULL,
    organization_uuid BINARY(16) NOT NULL,
    contract_uuid BINARY(16) NOT NULL,
    product_uuid BINARY(16) NOT NULL,
    quantity DECIMAL(10,4) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    PRIMARY KEY (uuid),
    INDEX idx_cpi_contract (contract_uuid),
    CONSTRAINT fk_cpi_contract FOREIGN KEY (contract_uuid) REFERENCES contracts(uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE contracts ADD COLUMN total_value DECIMAL(12,2) DEFAULT NULL;
