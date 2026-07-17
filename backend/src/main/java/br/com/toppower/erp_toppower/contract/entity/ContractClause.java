package br.com.toppower.erp_toppower.contract.entity;

import br.com.toppower.erp_toppower.common.entity.OrganizationScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cláusula de um contrato de prestação de serviços.
 *
 * <p>Entidade filha de {@link Contract} seguindo a convenção do projeto:
 * <strong>sem relacionamento JPA</strong> ({@code @OneToMany}/{{@code @ManyToOne}}).
 * A referência ao contrato pai é armazenada em {@link #contractId} (coluna
 * simples, sem FK física) e carregada pelo serviço via
 * {@code ContractClauseRepository.findByContractIdOrderByClauseNumberAsc(...)}.</p>
 *
 * <p><b>Organization-scoped</b>: herda de {@link OrganizationScopedEntity}
 * para isolamento multi-tenant. O {@code organizationId} é preenchido
 * automaticamente pelo {@code OrganizationEntityListener} no persist.</p>
 *
 * <p>A cláusula de número 1 (DO OBJETO) pode referenciar um
 * {@code ServiceTemplate} via {@link #serviceTemplateId}, copiando sua
 * descrição para {@link #content} no momento da criação do contrato.
 * As demais cláusulas (2–11) não utilizam esse campo.</p>
 */
@Entity
@Table(
        name = "contract_clauses",
        indexes = {
                @Index(name = "idx_contract_clause_contract", columnList = "contract_id"),
                @Index(name = "idx_contract_clause_org", columnList = "organization_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ContractClause extends OrganizationScopedEntity {

    /**
     * ID do contrato pai ao qual esta cláusula pertence.
     * Imutável após a criação ({@code updatable = false}).
     */
    @Column(name = "contract_id", nullable = false, updatable = false)
    private Long contractId;

    /**
     * Número da cláusula (1 a 11). Define a ordem de exibição.
     */
    @Column(name = "clause_number", nullable = false)
    private Integer clauseNumber;

    /**
     * Título da cláusula (ex.: "DO OBJETO", "DA VIGÊNCIA").
     */
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /**
     * Texto completo da cláusula em HTML ou texto puro. Persistido como
     * {@code TEXT} para suportar formatação rica e textos longos.
     */
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    /**
     * Referência ao {@code ServiceTemplate} que originou esta cláusula.
     * Preenchido <b>apenas</b> na cláusula 1 (DO OBJETO); nulo nas demais.
     * Mantido para rastreabilidade — o conteúdo é uma cópia (snapshot)
     * da descrição do template no momento da criação do contrato.
     */
    @Column(name = "service_template_id")
    private Long serviceTemplateId;
}