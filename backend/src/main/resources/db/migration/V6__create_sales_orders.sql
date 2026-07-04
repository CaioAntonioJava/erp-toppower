-- =============================================================================
-- V6__create_sales_orders.sql
--
-- Cria as tabelas do agregado `SalesOrder` (pedido de venda):
--   * `sales_orders`       — cabeçalho do pedido
--   * `sales_order_items`  — linhas de produto do pedido
--
-- Contexto:
--   O pedido de venda é a conversão operacional de uma proposta comercial
--   (Quotation) em um compromisso de venda. Pode nascer de duas formas:
--     1) Conversão de uma Quotation ATIVA (snapshot + rastreabilidade via
--        quotation_uuid/quotation_number);
--     2) Criação direta, sem proposta de origem (quotation_* nulos).
--
--   Diferente da proposta, o pedido NÃO possui margem de lucro
--   (profit_margin). O pedido é o documento externo enviado ao cliente
--   (PDF), e a margem é informação interna de precificação, mantida
--   apenas na Quotation. O total do pedido reflete o que o cliente paga:
--     total = (subtotal − desconto global) + frete
--
--   A numeração é gerada pela aplicação (iniciando em 1000 e incrementando
--   +1 por pedido), e a coluna `number` é BIGINT NOT NULL UNIQUE.
--
-- Importante:
--   O Hibernate com `ddl-auto=update` cria as colunas mas NÃO garante
--   UNIQUE CONSTRAINTS e ÍNDICES extras em todos os cenários, por isso
--   esta migration cria as tabelas explicitamente. As tabelas usam
--   `CREATE TABLE IF NOT EXISTS` para serem idempotentes em ambientes
--   de dev (spring.sql.init.mode=always).
--
-- Convenções do projeto:
--   * UUIDs são armazenados como BINARY(16) (mesmo formato das demais
--     tabelas — ver V5__seed_initial_data.sql, que usa UNHEX(...)).
--   * Auditoria (created_at, updated_at, created_by, updated_by) segue
--     o mesmo padrão das demais tabelas (Hibernate @AuditingEntityListener).
--   * As FKs lógicas (customer_uuid, company_uuid, seller_uuid,
--     carrier_uuid, product_uuid, quotation_uuid) NÃO são FKs físicas
--     — o projeto não declara relacionamentos JPA, apenas referências
--     por UUID.
-- =============================================================================

CREATE TABLE IF NOT EXISTS sales_orders (
    uuid                BINARY(16)      NOT NULL,
    number              BIGINT          NOT NULL,
    order_date          DATE            NOT NULL,
    customer_uuid       BINARY(16)      NULL,
    company_uuid        BINARY(16)      NULL,
    attention           VARCHAR(150)    NULL,
    seller_uuid         BINARY(16)      NOT NULL,
    discount_type       VARCHAR(20)     NULL,
    discount            DECIMAL(10, 2)  NULL,
    payment_condition   VARCHAR(50)     NULL,
    notes               VARCHAR(2000)   NULL,
    carrier_uuid        BINARY(16)      NULL,
    freight_type        VARCHAR(10)     NULL,
    freight_value       DECIMAL(10, 2)  NULL,
    status              VARCHAR(20)     NOT NULL,
    quotation_uuid      BINARY(16)      NULL,
    quotation_number    BIGINT          NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
                                            ON UPDATE CURRENT_TIMESTAMP,
    created_by          VARCHAR(100)    NULL,
    updated_by          VARCHAR(100)    NULL,
    PRIMARY KEY (uuid),
    UNIQUE KEY uk_sales_order_number (number)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- =============================================================================
-- Linhas de produto do pedido
-- =============================================================================

CREATE TABLE IF NOT EXISTS sales_order_items (
    uuid                BINARY(16)      NOT NULL,
    sales_order_uuid    BINARY(16)      NOT NULL,
    product_uuid        BINARY(16)      NOT NULL,
    quantity            DECIMAL(10, 4)  NOT NULL,
    unit_price          DECIMAL(10, 2)  NOT NULL,
    discount_type       VARCHAR(20)     NULL,
    discount            DECIMAL(10, 2)  NULL,
    total_price         DECIMAL(10, 2)  NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
                                            ON UPDATE CURRENT_TIMESTAMP,
    created_by          VARCHAR(100)    NULL,
    updated_by          VARCHAR(100)    NULL,
    PRIMARY KEY (uuid),
    KEY idx_sales_order_item_order (sales_order_uuid)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;