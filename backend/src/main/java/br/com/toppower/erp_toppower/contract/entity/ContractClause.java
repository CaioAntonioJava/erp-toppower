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
 * Linha de cláusula de um contrato: uma descrição livre de uma das
 * cláusulas contratuais.
 *
 * <p>Um contrato pode ter <b>várias</b> cláusulas, cada uma em uma linha
 * deste agregado. Cada item pertence a um único {@link Contract},
 * identificado por {@link #contractId}. A relação não é mapeada via JPA
 * (o projeto não utiliza relacionamentos JPA para coleções); o serviço
 * carrega os itens com um {@code findByContractId} no repositório.</p>
 */
@Entity
@Table(
        name = "contract_clauses",
        indexes = {
                @Index(name = "idx_cc_contract", columnList = "contract_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ContractClause extends OrganizationScopedEntity {

    /**
     * ID do {@link Contract} ao qual esta cláusula pertence.
     * Imutável após a criação ({@code updatable = false}).
     */
    @Column(name = "contract_id", nullable = false, updatable = false)
    private Long contractId;

    /**
     * Descrição da cláusula contratual (texto livre).
     * Obrigatória.
     */
    @Column(name = "description", nullable = false, length = 4000)
    private String description;
}
