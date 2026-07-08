package br.com.toppower.erp_toppower.sales.technicalproposal.entity;

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
 * Linha de serviço de uma proposta técnica: uma descrição de serviço
 * prestado com seu preço correspondente.
 *
 * <p>Cada item pertence a uma única {@link TechnicalProposal}, identificada
 * por {@link #technicalProposalUuid}. A relação não é mapeada via JPA (o
 * projeto não utiliza relacionamentos JPA para coleções); o serviço carrega
 * os itens com um {@code findByTechnicalProposalUuid} no repositório de
 * itens.</p>
 *
 * <p>O preço é o valor do serviço prestado — pode ser nulo quando o
 * serviço é gratuito/incluso. Não há cálculo adicional sobre a linha (sem
 * desconto nem quantidade); o valor informado entra diretamente no
 * somatório do subtotal da proposta.</p>
 */
@Entity
@Table(
        name = "technical_proposal_service_items",
        indexes = {
                @Index(name = "idx_tp_service_item_proposal", columnList = "technical_proposal_uuid")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class TechnicalProposalServiceItem extends OrganizationScopedEntity {

    /**
     * UUID da {@link TechnicalProposal} à qual este item pertence.
     * Imutável após a criação ({@code updatable = false}).
     */
    @Column(name = "technical_proposal_uuid", nullable = false, updatable = false)
    private UUID technicalProposalUuid;

    /**
     * Descrição do serviço prestado (texto livre). Obrigatória.
     */
    @Column(name = "description", nullable = false, length = 2000)
    private String description;

    /**
     * Preço do serviço prestado, <b>com a margem de lucro</b> da proposta
     * já embutida (snapshot final). Opcional — pode ser nulo quando o
     * serviço é gratuito/incluso. Entra no somatório do subtotal da
     * proposta.
     *
     * <p>Para edição o frontend usa {@link #basePrice} (preço original
     * enviado pelo usuário, sem margem) como ponto de partida, evitando
     * reaplicar a margem sobre o snapshot.</p>
     */
    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * Preço original do serviço, <b>sem a margem de lucro</b> aplicada.
     * É o valor enviado pelo usuário no formulário e o ponto de partida
     * para a aplicação da margem no momento da criação/atualização.
     *
     * <p>Persistido separadamente de {@link #price} para que a edição
     * da proposta não reaplique a margem sobre o snapshot já majorado.</p>
     */
    @Column(name = "base_price", precision = 10, scale = 2)
    private BigDecimal basePrice;
}