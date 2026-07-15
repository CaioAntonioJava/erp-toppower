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

/**
 * Linha de serviço de uma proposta técnica: uma descrição de serviço
 * prestado com seu preço correspondente.
 *
 * <p>Cada item pertence a uma única {@link TechnicalProposal}, identificada
 * por {@link #technicalProposalId}. A relação não é mapeada via JPA (o
 * projeto não utiliza relacionamentos JPA para coleções); o serviço carrega
 * os itens com um {@code findByTechnicalProposalId} no repositório de
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
                @Index(name = "idx_tp_service_item_proposal", columnList = "technical_proposal_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class TechnicalProposalServiceItem extends OrganizationScopedEntity {

    /**
     * ID da {@link TechnicalProposal} à qual este item pertence.
     * Imutável após a criação ({@code updatable = false}).
     */
    @Column(name = "technical_proposal_id", nullable = false, updatable = false)
    private Long technicalProposalId;

    /**
     * Descrição do serviço prestado (HTML formatado, texto livre). Opcional —
     * pode ser nula quando o serviço é informado apenas pelo preço. O conteúdo
     * é editado no frontend via editor de texto rico e persistido como HTML,
     * compatível com a renderização do PDF ({@code th:utext}).
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Preço do serviço prestado. Opcional — pode ser nulo quando o
     * serviço é gratuito/incluso. Entra no somatório do subtotal da
     * proposta.
     */
    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * Categoria do serviço no catálogo (ex.: "EXECUÇÃO_SPDA").
     * Opcional — presente apenas quando o item foi criado a partir
     * de um template do catálogo (modo CATALOG).
     */
    @Column(name = "category", length = 50)
    private String category;

    /**
     * ID do {@code ServiceTemplate} que originou este item.
     * Opcional — presente apenas quando o item foi criado a partir
     * de um template do catálogo (modo CATALOG).
     */
    @Column(name = "service_template_id")
    private Long serviceTemplateId;
}