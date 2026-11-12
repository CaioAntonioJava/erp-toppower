package br.com.toppower.erp_toppower.contractfinances.entity;

import br.com.toppower.erp_toppower.common.entity.OrganizationScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Registro de controle do financeiro de um contrato: criado
 * automaticamente pelo módulo {@code contractfinances} quando o contrato
 * é criado (e sincronizado quando o contrato é alterado), guardando o
 * retrato dos dados comerciais do contrato na data do vínculo:
 *
 * <ul>
 *   <li>{@link #contractCode} — código comercial formatado
 *       ({@code CL-001-2026});</li>
 *   <li>{@link #clientName}/{@link #clientType}/{@link #clientCode} —
 *       cliente (PF ou PJ) resolvido no momento da criação;</li>
 *   <li>{@link #contractDate} — data de vigência do contrato;</li>
 *   <li>{@link #contractValue} — valor (preço) do contrato.</li>
 * </ul>
 *
 * <p>Os lançamentos do financeiro ({@link ContractFinanceEntry}) ficam
 * pendurados neste controle. O vínculo com o contrato é feito por
 * {@link #contractId}, sem FK física (convenção do projeto), com
 * controles únicos por contrato dentro da organização.</p>
 *
 * <p><b>Organization-scoped</b>: herda de {@link OrganizationScopedEntity}
 * para o isolamento multi-tenant via {@code organizationFilter}.</p>
 */
@Entity
@Table(
        name = "contract_finance_controls",
        indexes = {
                @Index(name = "idx_cfin_control_code", columnList = "contract_code")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_cfin_control_org_contract",
                        columnNames = {"organization_id", "contract_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ContractFinanceControl extends OrganizationScopedEntity {

    /**
     * ID do {@code Contract} controlado. Sem FK física (convenção do
     * projeto). Imutável: um controle pertence a um único contrato.
     */
    @Column(name = "contract_id", nullable = false, updatable = false)
    private Long contractId;

    /**
     * Código comercial formatado do contrato
     * ({@code <prefix>-<seq>-<year>}), denormalizado para exibição e busca
     * sem join no módulo de contratos.
     */
    @Column(name = "contract_code", nullable = false, updatable = false, length = 30)
    private String contractCode;

    /**
     * Tipo do cliente no momento da criação do controle
     * ({@code CUSTOMER} = pessoa física, {@code COMPANY} = pessoa
     * jurídica).
     */
    @Column(name = "client_type", length = 20)
    private String clientType;

    /** Código do cliente (CLI000001 / EMP000001), denormalizado. */
    @Column(name = "client_code", length = 50)
    private String clientCode;

    /** Nome do cliente (PF ou nome fantasia/razão social da PJ), denormalizado. */
    @Column(name = "client_name", length = 200)
    private String clientName;

    /** Data de vigência do contrato (retrato; sincronizada no update). */
    @Column(name = "contract_date")
    private LocalDate contractDate;

    /** Valor (preço) do contrato (retrato; sincronizado no update). */
    @Column(name = "contract_value", precision = 12, scale = 2)
    private BigDecimal contractValue;
}
