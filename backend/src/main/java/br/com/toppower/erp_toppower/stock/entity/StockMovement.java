package br.com.toppower.erp_toppower.stock.entity;

import br.com.toppower.erp_toppower.common.entity.OrganizationScopedEntity;
import br.com.toppower.erp_toppower.stock.enums.MovementSource;
import br.com.toppower.erp_toppower.stock.enums.MovementType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Diário (ledger) de movimentações de estoque — o registro imutável de
 * toda alteração de saldo de um produto. É a fonte da verdade para
 * auditoria e estornos.
 *
 * <p><b>Invariante de saldo:</b> {@code stockAfter = stockBefore + quantityChange}.
 * A coluna {@code Product.stockQuantity} é atualizada em sincronia com a
 * última movimentação registrada para o produto, sempre dentro da mesma
 * transação que cria a movimentação.</p>
 *
 * <p><b>Estornos:</b> uma movimentação estornada é marcada com
 * {@code reversed=true} e uma nova movimentação complementar é criada
 * ({@link MovementType#ESTORNO_SAIDA} ou
 * {@link MovementType#ESTORNO_ENTRADA}), referenciando a original via
 * {@code reversalOfUuid}. Isso impede estornos duplicados e preserva o
 * histórico completo.</p>
 *
 * <p><b>Rastreabilidade:</b> {@code source} + {@code sourceUuid} +
 * {@code sourceNumber} ligam a movimentação ao documento de origem
 * (ex.: pedido de venda), permitindo estornar todas as baixas de uma
 * venda ao cancelá-la, sem lógica duplicada no módulo de origem.</p>
 */
@Entity
@Table(name = "stock_movements")
@Getter
@Setter
@NoArgsConstructor
public class StockMovement extends OrganizationScopedEntity {

    /** Produto cujo saldo foi alterado. Referência lógica por UUID (sem FK física). */
    @Column(name = "product_uuid", nullable = false)
    private UUID productUuid;

    /**
     * Variação aplicada ao saldo. Sinal segue o {@link MovementType}:
     * positiva para entradas/estornos de saída, negativa para saídas/
     * estornos de entrada.
     */
    @Column(name = "quantity_change", nullable = false, precision = 10, scale = 4)
    private BigDecimal quantityChange;

    /** Saldo do produto imediatamente antes desta movimentação. */
    @Column(name = "stock_before", nullable = false, precision = 10, scale = 4)
    private BigDecimal stockBefore;

    /** Saldo do produto imediatamente depois desta movimentação. */
    @Column(name = "stock_after", nullable = false, precision = 10, scale = 4)
    private BigDecimal stockAfter;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private MovementType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 30)
    private MovementSource source;

    /**
     * UUID do documento de origem (ex.: {@code SalesOrder.uuid}). Nulo
     * apenas para movimentações sem origem rastreável (ajustes manuais
     * avulsos futuros).
     */
    @Column(name = "source_uuid")
    private UUID sourceUuid;

    /**
     * Número legível do documento de origem (ex.: {@code SalesOrder.number}).
     * Snapshot para exibição em históricos sem precisar reconsultar a origem.
     */
    @Column(name = "source_number")
    private Long sourceNumber;

    /** Observação livre descrevendo o motivo da movimentação. */
    @Column(name = "reason", length = 500)
    private String reason;

    /**
     * Indica que esta movimentação foi estornada por uma movimentação
     * complementar. Uma vez {@code true}, não pode ser estornada novamente.
     */
    @Column(name = "reversed", nullable = false)
    private boolean reversed = false;

    /**
     * Quando esta movimentação é um estorno, aponta para a movimentação
     * original que está sendo desfeita. Nulo para movimentações primárias.
     */
    @Column(name = "reversal_of_uuid")
    private UUID reversalOfUuid;

    /**
     * Construtor de conveniência para movimentações primárias (não-estorno).
     * {@code reversed} fica {@code false} e {@code reversalOfUuid} nulo.
     */
    public StockMovement(UUID productUuid, BigDecimal quantityChange,
                         BigDecimal stockBefore, BigDecimal stockAfter,
                         MovementType type, MovementSource source,
                         UUID sourceUuid, Long sourceNumber, String reason) {
        this.productUuid = productUuid;
        this.quantityChange = quantityChange;
        this.stockBefore = stockBefore;
        this.stockAfter = stockAfter;
        this.type = type;
        this.source = source;
        this.sourceUuid = sourceUuid;
        this.sourceNumber = sourceNumber;
        this.reason = reason;
        this.reversed = false;
        this.reversalOfUuid = null;
    }
}