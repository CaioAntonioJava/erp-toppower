package br.com.toppower.erp_toppower.sales.salesorder.entity;

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
 * Linha de um pedido de venda: um produto (referenciado por ID) com
 * sua quantidade, preço unitário e margem de lucro próprios.
 *
 * <p>Cada item pertence a um único {@link SalesOrder}, identificado por
 * {@link #salesOrderId}. A relação não é mapeada via JPA (o projeto
 * não utiliza relacionamentos JPA para coleções); o serviço carrega os
 * itens com um {@code findBySalesOrderId} no repositório de itens.</p>
 *
 * <p>O campo {@link #totalPrice} armazena o total da linha, resultado
 * de {@code unitPrice * quantity}, onde {@code unitPrice} já reflete a
 * margem de lucro aplicada (a do item quando informada, senão a do
 * cabeçalho). Esse é o valor que entra no somatório do {@code subtotal}
 * do {@code SalesOrder}.</p>
 *
 * <p>Na conversão a partir de uma {@code Quotation}, os preços dos itens
 * já vêm com a margem embutida (snapshot); nesse caso {@link #profitMargin}
 * é copiada da cotação para rastreabilidade, mas não é reaplicada.</p>
 */
@Entity
@Table(
        name = "sales_order_items",
        indexes = {
                @Index(name = "idx_sales_order_item_order", columnList = "sales_order_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class SalesOrderItem extends OrganizationScopedEntity {

    /**
     * ID do {@link SalesOrder} ao qual este item pertence.
     * Imutável após a criação ({@code updatable = false}).
     */
    @Column(name = "sales_order_id", nullable = false, updatable = false)
    private Long salesOrderId;

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
     * Preço unitário do produto <b>no momento da emissão</b> do pedido
     * (snapshot), já com a margem de lucro efetiva embutida. Não é
     * atualizado quando o preço do {@code Product} muda depois.
     */
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    /**
     * Preço unitário original enviado pelo usuário (sem margem de lucro).
     * Preservado para que a edição do pedido não reaplique a margem
     * sobre o snapshot já majorado, e para rastreabilidade.
     */
    @Column(name = "base_unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseUnitPrice;

    /**
     * Margem de lucro (em %) aplicada especificamente a esta linha.
     * Quando informada, sobrescreve a margem do cabeçalho do pedido para
     * este item. Nula quando a linha usa a margem do cabeçalho (ou
     * quando o pedido veio de conversão, já com a margem embutida no
     * snapshot).
     */
    @Column(name = "profit_margin", precision = 5, scale = 2)
    private BigDecimal profitMargin;

    /**
     * Total da linha: {@code unitPrice * quantity}, sendo
     * {@code unitPrice} já majorado pela margem efetiva.
     *
     * <p>Calculado e persistido pelo serviço no momento de criar ou
     * atualizar o item. É este o valor que entra no somatório do
     * {@code subtotal} do pedido.</p>
     */
    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;
}