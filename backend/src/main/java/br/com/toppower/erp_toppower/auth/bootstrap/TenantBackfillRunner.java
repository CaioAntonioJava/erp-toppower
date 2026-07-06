package br.com.toppower.erp_toppower.auth.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Runner que prepara as tabelas de negócio para o multi-tenancy DEPOIS do
 * Hibernate criar as colunas {@code tenant_uuid} (nullable) via
 * {@code ddl-auto=update}.
 *
 * <p>Faz três coisas, de forma idempotente (segura para re-boot com
 * {@code spring.sql.init.mode=always}):</p>
 * <ol>
 *   <li><b>Backfill</b>: seta {@code tenant_uuid} de todos os registros pré-
 *       multi-tenancy para o tenant default (UUID determinístico
 *       {@code 00000000-0000-4000-8000-0000000000a1}).</li>
 *   <li><b>NOT NULL</b>: converte a coluna para {@code NOT NULL} (após o
 *       backfill não há NULLs). Re-executável: {@code ALTER TABLE ... MODIFY}
 *       é idempotente.</li>
 *   <li><b>Índice</b>: cria {@code idx_tenant (tenant_uuid)} se ainda não
 *       existir, para acelerar o filtro de tenant.</li>
 * </ol>
 *
 * <p>Roda após o Hibernate (ordem {@link Order} alta), então as colunas já
 * existem. Não cria as colunas — isso é responsabilidade do Hibernate
 * (TenantScopedEntity as declara, e ddl-auto=update as materializa).</p>
 */
@Component
@Order(10)
public class TenantBackfillRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(TenantBackfillRunner.class);

    /** Hex do UUID 00000000-0000-4000-8000-0000000000a1 (32 chars). */
    private static final String DEFAULT_TENANT_HEX = "000000000000400080000000000000a1";

    /** Tabelas de negócio que herdam TenantScopedEntity (precisam de tenant_uuid). */
    private static final List<String> TENANT_TABLES = List.of(
            "companies", "customers", "sellers", "products",
            "carriers", "suppliers", "quotations", "quotation_items",
            "sales_orders", "sales_order_items", "technical_proposals",
            "technical_proposal_objectives", "technical_proposal_product_items",
            "technical_proposal_service_items", "stock_movements"
    );

    private final JdbcTemplate jdbcTemplate;

    public TenantBackfillRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        for (String table : TENANT_TABLES) {
            if (!columnExists(table, "tenant_uuid")) {
                // Entidade ainda não materializada pelo Hibernate (improvável no
                // fluxo normal, mas possível em testes/migrações parciais). Pula
                // silenciosamente — o Hibernate criará a coluna nullable e o
                // próximo boot completará o backfill.
                log.debug("Tabela '{}' sem coluna tenant_uuid ainda — pulando backfill.", table);
                continue;
            }
            backfill(table);
            makeNotNull(table);
            ensureIndex(table);
        }
    }

    private boolean columnExists(String table, String column) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
                Integer.class, table, column);
        return count != null && count > 0;
    }

    private void backfill(String table) {
        int updated = jdbcTemplate.update(
                "UPDATE " + table + " SET tenant_uuid = UNHEX('" + DEFAULT_TENANT_HEX + "') "
                        + "WHERE tenant_uuid IS NULL");
        if (updated > 0) {
            log.info("Backfill de tenant_uuid em '{}': {} registros atualizados.", table, updated);
        }
    }

    private void makeNotNull(String table) {
        // Idempotente: MODIFY COLUMN é no-op se já é NOT NULL.
        jdbcTemplate.execute(
                "ALTER TABLE " + table + " MODIFY COLUMN tenant_uuid BINARY(16) NOT NULL");
    }

    private void ensureIndex(String table) {
        String indexName = "idx_tenant";
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.statistics "
                        + "WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?",
                Integer.class, table, indexName);
        if (count != null && count > 0) {
            return; // índice já existe
        }
        jdbcTemplate.execute("CREATE INDEX " + indexName + " ON " + table + " (tenant_uuid)");
        log.info("Índice idx_tenant criado em '{}'.", table);
    }
}