package br.com.toppower.erp_toppower.contract.entity;

import br.com.toppower.erp_toppower.common.entity.OrganizationScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Item de produto de um contrato: referência a um produto cadastrado +
 * quantidade.
 *
 * <p>Um contrato pode ter <b>vários</b> itens de produto, cada um em uma
 * linha deste agregado. Cada item pertence a um único {@link Contract},
 * identificado por {@link #contractUuid}. A relação não é mapeada via JPA
 * (o projeto não utiliza relacionamentos JPA para coleções); o serviço
 * carrega os itens com um {@code findByContractUuid} no repositório.</p>
 *
 * <p>Diferente da Proposta Técnica, este item não possui preço, desconto
 * ou margem — apenas a referência ao produto e a quantidade contratada.</p>
 */
@Entity
@Table(
        name = "contract_product_items",
        indexes = {
                @Index(name = "idx_cpi_contract", columnList = "contract_uuid")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ContractProductItem extends OrganizationScopedEntity {

    /**
     * UUID do {@link Contract} ao qual este item de produto pertence.
     * Imutável após a criação ({@code updatable = false}).
     */
    @Column(name = "contract_uuid", nullable = false, updatable = false)
    private UUID contractUuid;

    /**
     * UUID do produto referenciado (tabela {@code products}).
     * Obrigatório.
     */
    @Column(name = "product_uuid", nullable = false)
    private UUID productUuid;

    /**
     * Quantidade contratada do produto. Obrigatória, com 4 casas
     * decimais de precisão.
     */
    @Column(name = "quantity", nullable = false, precision = 10, scale = 4)
    private BigDecimal quantity;
}
