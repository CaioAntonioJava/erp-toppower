package br.com.toppower.erp_toppower.contract.entity;

import br.com.toppower.erp_toppower.common.entity.OrganizationScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Item de serviço de um contrato: uma descrição livre de um dos serviços
 * prestados.
 *
 * <p>Um contrato pode ter <b>vários</b> itens de serviço, cada um em uma
 * linha deste agregado. Cada item pertence a um único {@link Contract},
 * identificado por {@link #contractUuid}. A relação não é mapeada via JPA
 * (o projeto não utiliza relacionamentos JPA para coleções); o serviço
 * carrega os itens com um {@code findByContractUuid} no repositório.</p>
 *
 * <p>Diferente da Proposta Técnica, este item não possui preço — apenas
 * descrição textual.</p>
 */
@Entity
@Table(
        name = "contract_service_items",
        indexes = {
                @Index(name = "idx_csi_contract", columnList = "contract_uuid")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ContractServiceItem extends OrganizationScopedEntity {

    /**
     * UUID do {@link Contract} ao qual este item de serviço pertence.
     * Imutável após a criação ({@code updatable = false}).
     */
    @Column(name = "contract_uuid", nullable = false, updatable = false)
    private UUID contractUuid;

    /**
     * Descrição do serviço (texto livre). Obrigatória.
     */
    @Column(name = "description", nullable = false, length = 2000)
    private String description;
}
