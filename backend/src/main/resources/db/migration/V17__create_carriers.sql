-- =============================================================================
-- V17__create_carriers.sql
--
-- Cria a tabela `carriers` — cadastro de transportadoras (Carrier).
--
-- Contexto:
--   O módulo Carrier foi reintroduzido ao projeto. A V15__drop_carriers.sql
--   removeu o schema legado (tabela + colunas órfãs em quotations/sales_orders)
--   e, com `ddl-auto=update`, o Hibernate recriaria a tabela — PORÉM o Spring
--   executa os scripts SQL (`spring.sql.init.schema-locations`) DEPOIS do
--   Hibernate na inicialização. Assim, na ordem efetiva do boot:
--     1) Hibernate `update` cria a tabela `carriers`;
--     2) V15 roda e DROPa a tabela recém-criada;
--     3) a tabela fica ausente pelo restante do boot -> erro 1146.
--   Esta migration cria a tabela explicitamente, após a V15/V16, de forma
--   idempotente (`CREATE TABLE IF NOT EXISTS`), garantindo a existência
--   independentemente da ordem Hibernate/SQL init.
--
--   O Hibernate `ddl-auto=update` continua responsável por manter as
--   colunas em sincronia com a entidade; aqui criamos apenas a tabela.
--
-- Convenções do projeto (mesmo padrão da V9__create_stock_movements.sql):
--   * UUID como BINARY(16);
--   * Auditoria created_at/updated_at/created_by/updated_by;
--   * Sem FKs físicas.
-- =============================================================================

CREATE TABLE IF NOT EXISTS carriers (
    id            BIGINT          NOT NULL AUTO_INCREMENT,
    name            VARCHAR(200)   NOT NULL,
    status          VARCHAR(20)    NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
                                            ON UPDATE CURRENT_TIMESTAMP,
    created_by      VARCHAR(100)    NULL,
    updated_by      VARCHAR(100)    NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;