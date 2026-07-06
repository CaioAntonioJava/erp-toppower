-- =============================================================================
-- V9__create_stock_movements.sql
--
-- Cria a tabela `stock_movements` — o diário (ledger) de movimentações de
-- estoque do sistema. Toda alteração de `products.stock_quantity` passa a
-- ser registrada aqui, com saldo antes/depois, tipo de movimentação,
-- origem (módulo de negócio) e rastreabilidade ao documento que a gerou.
--
-- Contexto:
--   Historicamente o estoque era apenas uma coluna manual em `products`,
--   setada no cadastro/edição do produto. Com a baixa automática na
--   finalização do pedido de venda (e, no futuro, outros módulos como
--   compras, devoluções e ajustes de inventário), centraliza-se toda
--   mutação em um único diário, habilitando:
--     * Auditoria completa (quem/quando/porquê mudou o saldo);
--     * Estorno agrupado por origem (cancelamento de venda devolve o
--       estoque lendo as movimentações originais);
--     * Extensibilidade — novos módulos só precisam chamar o StockService.
--
--   A coluna `products.stock_quantity` permanece como saldo corrente
--   (cache derivado do diário), atualizada em sincronia dentro da mesma
--   transação que cria a movimentação.
--
-- Importante:
--   O Hibernate com `ddl-auto=update` cria as colunas mas NÃO garante
--   ÍNDICES extras, por isso esta migration cria a tabela explicitamente.
--   Usa `CREATE TABLE IF NOT EXISTS` para ser idempotente em ambientes
--   de dev (spring.sql.init.mode=always).
--
-- Convenções do projeto:
--   * UUIDs são armazenados como BINARY(16) (mesmo formato das demais
--     tabelas — ver V5__seed_initial_data.sql).
--   * Auditoria (created_at, updated_at, created_by, updated_by) segue
--     o mesmo padrão das demais tabelas (Hibernate @AuditingEntityListener).
--   * As FKs lógicas (product_uuid, source_uuid, reversal_of_uuid) NÃO
--     são FKs físicas — o projeto não declara relacionamentos JPA, apenas
--     referências por UUID.
-- =============================================================================

CREATE TABLE IF NOT EXISTS stock_movements (
    uuid                BINARY(16)      NOT NULL,
    product_uuid        BINARY(16)      NOT NULL,
    quantity_change     DECIMAL(10, 4)  NOT NULL,
    stock_before        DECIMAL(10, 4)  NOT NULL,
    stock_after         DECIMAL(10, 4)  NOT NULL,
    type                VARCHAR(20)     NOT NULL,
    source              VARCHAR(30)     NOT NULL,
    source_uuid         BINARY(16)      NULL,
    source_number       BIGINT          NULL,
    reason              VARCHAR(500)    NULL,
    reversed            BOOLEAN         NOT NULL DEFAULT FALSE,
    reversal_of_uuid    BINARY(16)      NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
                                            ON UPDATE CURRENT_TIMESTAMP,
    created_by          VARCHAR(100)    NULL,
    updated_by          VARCHAR(100)    NULL,
    PRIMARY KEY (uuid),
    KEY idx_stock_movement_product (product_uuid),
    KEY idx_stock_movement_source (source, source_uuid)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;