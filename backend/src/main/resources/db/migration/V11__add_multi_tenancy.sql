-- =============================================================================
-- V11__add_multi_tenancy.sql
--
-- Introduz multi-tenancy no ERP via discriminator column (tenant_uuid) em todas
-- as tabelas de dados de negócio. Cada tenant = uma empresa operadora (CNPJ
-- próprio) cujos cadastros ficam isolados dos demais tenants.
--
-- Esta migration é responsável APENAS por:
--   1) Criar a tabela `tenants` (empresas operadoras/donas do dado).
--   2) Criar a tabela `user_tenants` (vínculo N:N usuário↔tenant).
--   3) Semear o tenant default (UUID determinístico usado pelo BootstrapRunner).
--
-- A coluna `tenant_uuid` nas tabelas de negócio (companies, customers, etc.)
-- NÃO é criada aqui — ela é criada automaticamente pelo Hibernate
-- (ddl-auto=update) a partir da mapped superclass TenantScopedEntity, como
-- NULLABLE. O backfill (SET tenant_uuid = <default>), a conversão para
-- NOT NULL e a criação dos índices são feitos no BootstrapRunner, que roda
-- DEPOIS do Hibernate (e portanto depois das colunas existirem).
--
-- Motivo desta divisão:
--   * `defer-datasource-initialization=true` faz o spring.sql.init rodar ANTES
--     do Hibernate. Se esta migration tentasse ALTER TABLE ... ADD COLUMN
--     nas tabelas de negócio, isso funcionaria no 1º boot, mas falharia nos
--     boots subsequentes (MySQL não suporta "ADD COLUMN IF NOT EXISTS").
--   * Deixar o Hibernate criar as colunas (idempotente por natureza) e fazer
--     o backfill/NOT NULL em Java (no BootstrapRunner, que roda pós-Hibernate)
--     resolve a idempotência de forma limpa e portável.
--
-- Convenções do projeto (mesmas do V9 e demais):
--   * UUIDs como BINARY(16).
--   * Auditoria (created_at, updated_at, created_by, updated_by).
--   * FKs lógicas (tenant_uuid, user_uuid) NÃO são FKs físicas — referências
--     por UUID apenas, sem CONSTRAINT FOREIGN KEY.
--   * Idempotente: CREATE TABLE IF NOT EXISTS / INSERT ... WHERE NOT EXISTS.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1) Tabela `tenants` — empresas operadoras (donas dos dados).
--    Inclui IE, IM, endereço embutido (address_*) — mesmo padrão do Company.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tenants (
    uuid                            BINARY(16)      NOT NULL,
    legal_name                      VARCHAR(200)    NOT NULL,
    trade_name                      VARCHAR(200)    NULL,
    code                            VARCHAR(50)     NOT NULL,
    cnpj                            VARCHAR(20)     NOT NULL,
    state_registration              VARCHAR(30)     NULL,
    state_registration_exempt       BOOLEAN         NOT NULL DEFAULT FALSE,
    municipal_registration          VARCHAR(30)     NULL,
    address_street                  VARCHAR(200)    NOT NULL,
    address_number                  VARCHAR(20)     NOT NULL,
    address_complement              VARCHAR(100)    NULL,
    address_neighborhood            VARCHAR(100)    NULL,
    address_city                    VARCHAR(100)    NOT NULL,
    address_state                   VARCHAR(2)      NOT NULL,
    address_zip_code                VARCHAR(9)      NOT NULL,
    status                          VARCHAR(20)     NOT NULL,
    created_at                      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
                                                        ON UPDATE CURRENT_TIMESTAMP,
    created_by                      VARCHAR(100)    NULL,
    updated_by                      VARCHAR(100)    NULL,
    PRIMARY KEY (uuid),
    UNIQUE KEY uk_tenants_code (code),
    UNIQUE KEY uk_tenants_cnpj (cnpj)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- -----------------------------------------------------------------------------
-- 2) Tabela `user_tenants` — vínculo N:N usuário↔tenant.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_tenants (
    uuid                BINARY(16)      NOT NULL,
    user_uuid           BINARY(16)      NOT NULL,
    tenant_uuid         BINARY(16)      NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
                                            ON UPDATE CURRENT_TIMESTAMP,
    created_by          VARCHAR(100)    NULL,
    updated_by          VARCHAR(100)    NULL,
    PRIMARY KEY (uuid),
    UNIQUE KEY uk_user_tenants_user_tenant (user_uuid, tenant_uuid),
    KEY idx_user_tenants_user (user_uuid),
    KEY idx_user_tenants_tenant (tenant_uuid)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- -----------------------------------------------------------------------------
-- 3) Seed do tenant default (UUID determinístico — referenciado pelo
--    BootstrapRunner e pelo backfill em Java).
--    00000000-0000-4000-8000-0000000000a1 -> hex 000000000000400080000000000000a1
-- -----------------------------------------------------------------------------
INSERT INTO tenants (uuid, legal_name, trade_name, code, cnpj, status,
                     created_at, updated_at, created_by, updated_by)
SELECT UNHEX('000000000000400080000000000000a1'),
       'TOPPOWER ENGENHARIA LTDA', 'TOPPOWER', 'TEN000001', '00000000000100', 'ATIVO',
       '2026-01-01 12:00:00', '2026-01-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (
    SELECT 1 FROM tenants WHERE uuid = UNHEX('000000000000400080000000000000a1')
);