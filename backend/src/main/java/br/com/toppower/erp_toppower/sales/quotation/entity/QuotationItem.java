package br.com.toppower.erp_toppower.sales.quotation.entity;

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
 * Linha de uma proposta comercial: um produto (referenciado por ID)
 * com sua quantidade, preço unitário e margem de lucro próprios.
 *
 * <p>Cada item pertence a uma única {@link Quotation}, identificada por
 * {@link #quotationId}. A relação não é mapeada via JPA (o projeto
 * não utiliza relacionamentos JPA para coleções); o serviço carrega
 * os itens com um {@code findByQuotationId} no repositório de itens.</p>
 *
 * <p>O campo {@link #totalPrice} armazena o total da linha, resultado
 * de {@code unitPrice * quantity}, onde {@code unitPrice} já reflete a
 * margem de lucro aplicada (a do item quando informada, senão a do
 * cabeçalho da proposta). Esse é o valor que entra no somatório do
 * {@code subtotal} da {@code Quotation}.</p>
 */
@Entity
@Table(
        name = "quotation_items",
        indexes = {
                @Index(name = "idx_quotation_item_quotation", columnList = "quotation_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class QuotationItem extends OrganizationScopedEntity {

    /**
     * ID da {@link Quotation} à qual este item pertence.
     * Imutável após a criação ({@code updatable = false}).
     */
    @Column(name = "quotation_id", nullable = false, updatable = false)
    private Long quotationId;

    /**
     * ID do {@code Product} referenciado pela linha.
     * Obrigatório.
     */
    @Column(name = "product_id", nullable = false)
    private Long productId;

    /**
     * Quantidade do produto nesta linha. Suporta até 4 casas decimais
     * para produtos vendidos em unidades fracionárias (ex.: metros).
     */
    @Column(name = "quantity", nullable = false, precision = 10, scale = 4)
    private BigDecimal quantity;

    /**
     * Preço unitário do produto <b>no momento da emissão</b> da proposta
     * (snapshot). Já reflete a margem de lucro da proposta embutida —
     * o valor final exibido no PDF e na listagem. Não é atualizado
     * quando o preço do {@code Product} muda depois.
     *
     * <p>Para edição o frontend usa {@link #baseUnitPrice} (preço
     * original enviado pelo usuário, sem margem) como ponto de partida,
     * evitando reaplicar a margem sobre o snapshot.</p>
     */
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    /**
     * Preço unitário original do produto, <b>sem a margem de lucro</b>
     * aplicada. É o valor enviado pelo usuário no formulário e o ponto
     * de partida para a aplicação da margem no momento da
     * criação/atualização.
     *
     * <p>Persistido separadamente de {@link #unitPrice} para que a
     * edição da proposta não reaplique a margem sobre o snapshot já
     * majorado. O cálculo do {@link #totalPrice} continua usando
     * {@code unitPrice} (com margem).</p>
     */
    @Column(name = "base_unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseUnitPrice;

    /**
     * Margem de lucro (em %) aplicada especificamente a esta linha.
     * Quando informada, sobrescreve a margem do cabeçalho da proposta
     * ({@link Quotation#getProfitMargin()}) para este item, permitindo
     * margens diferentes por produto em uma mesma lista. Quando nula,
     * o item usa a margem do cabeçalho.
     */
    @Column(name = "profit_margin", precision = 5, scale = 2)
    private BigDecimal profitMargin;

    /**
     * Total da linha: {@code unitPrice * quantity}, sendo
     * {@code unitPrice} já majorado pela margem efetiva (do item ou do
     * cabeçalho).
     *
     * <p>Calculado e persistido pelo serviço no momento de criar ou
     * atualizar o item. É este o valor que entra no somatório do
     * {@code subtotal} da proposta.</p>
     */
    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;
}
