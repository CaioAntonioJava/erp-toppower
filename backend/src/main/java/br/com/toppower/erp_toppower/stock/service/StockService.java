package br.com.toppower.erp_toppower.stock.service;

import br.com.toppower.erp_toppower.product.entity.Product;
import br.com.toppower.erp_toppower.product.repository.ProductRepository;
import br.com.toppower.erp_toppower.stock.dto.StockMovementResponse;
import br.com.toppower.erp_toppower.stock.entity.StockMovement;
import br.com.toppower.erp_toppower.stock.enums.MovementSource;
import br.com.toppower.erp_toppower.stock.enums.MovementType;
import br.com.toppower.erp_toppower.stock.exception.InsufficientStockException;
import br.com.toppower.erp_toppower.stock.exception.StockMovementNotFoundException;
import br.com.toppower.erp_toppower.stock.mapper.StockMovementMapper;
import br.com.toppower.erp_toppower.stock.repository.StockMovementRepository;
import br.com.toppower.erp_toppower.common.dto.PagedResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Fachada única para todas as alterações de saldo de estoque. Centraliza
 * o cálculo de before/after, a persistência do diário
 * ({@code stock_movements}) e a sincronia com {@code Product.stockQuantity}.
 *
 * <p><b>Regras de ouro:</b></p>
 * <ul>
 *   <li>Nenhum outro serviço altera {@code Product.stockQuantity} diretamente
 *       — tudo passa por aqui, garantindo diário completo e auditoria;</li>
 *   <li>Toda mutação roda dentro de uma transação do chamador
 *       ({@code @Transactional} propagado). Falha em qualquer item do lote
 *       reverte todo o conjunto (rollback);</li>
 *   <li>O produto é carregado com lock pessimista de escrita
 *       ({@code findByUuidForUpdate}) para serializar vendas concorrentes
 *       do mesmo produto;</li>
 *   <li>Estornos marcam a movimentação original como {@code reversed=true}
 *       e criam uma complementar ({@link MovementType#ESTORNO_SAIDA} /
 *       {@link MovementType#ESTORNO_ENTRADA}) referenciando-a via
 *       {@code reversalOfUuid}, impedindo estornos duplicados.</li>
 * </ul>
 *
 * <p>Estendível para futuros módulos (compras, devoluções, ajustes de
 * inventário): basta criar novas {@link MovementSource} e chamar
 * {@link #registrarEntrada} / {@link #registrarSaida}.</p>
 */
@Service
public class StockService {

    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;

    public StockService(ProductRepository productRepository,
                        StockMovementRepository stockMovementRepository) {
        this.productRepository = productRepository;
        this.stockMovementRepository = stockMovementRepository;
    }

    // ---------------------------------------------------------------------
    // Saídas
    // ---------------------------------------------------------------------

    /**
     * Registra uma saída de estoque (venda, perda, etc.). Reduz o saldo
     * do produto e cria uma movimentação {@link MovementType#SAIDA}.
     *
     * @throws InsufficientStockException se o saldo atual for menor que a
     *         quantidade solicitada — a transação do chamador sofre rollback.
     */
    @Transactional
    public StockMovement registrarSaida(UUID productUuid, BigDecimal quantity,
                                        MovementSource source, UUID sourceUuid,
                                        Long sourceNumber, String reason) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new IllegalArgumentException("Quantidade de saída deve ser positiva.");
        }
        Product p = productRepository.findByUuidForUpdate(productUuid)
                .orElseThrow(() -> new IllegalStateException(
                        "Produto " + productUuid + " não encontrado para baixa de estoque."));
        BigDecimal before = p.getStockQuantity();
        if (before.compareTo(quantity) < 0) {
            throw new InsufficientStockException(p.getName(), p.getCode(), before, quantity);
        }
        BigDecimal after = before.subtract(quantity);
        p.setStockQuantity(after);
        productRepository.save(p);

        StockMovement movement = new StockMovement(
                productUuid,
                quantity.negate(),
                before,
                after,
                MovementType.SAIDA,
                source,
                sourceUuid,
                sourceNumber,
                reason);
        return stockMovementRepository.save(movement);
    }

    /**
     * Registra saídas em lote para os itens de um documento (ex.: itens
     * de um pedido de venda). Processa item a item; se qualquer item
     * falhar (saldo insuficiente, produto inexistente), toda a transação
     * do chamador é revertida — nenhuma baixa parcial é persistida.
     */
    @Transactional
    public void registrarSaidaEmLote(List<SaidaItem> items, MovementSource source,
                                     UUID sourceUuid, Long sourceNumber, String reason) {
        for (SaidaItem item : items) {
            registrarSaida(item.productUuid(), item.quantity(),
                    source, sourceUuid, sourceNumber, reason);
        }
    }

    // ---------------------------------------------------------------------
    // Entradas (reservado p/ uso futuro — compras, devoluções de cliente)
    // ---------------------------------------------------------------------

    /**
     * Registra uma entrada de estoque. Aumenta o saldo do produto e cria
     * uma movimentação {@link MovementType#ENTRADA}.
     */
    @Transactional
    public StockMovement registrarEntrada(UUID productUuid, BigDecimal quantity,
                                          MovementSource source, UUID sourceUuid,
                                          Long sourceNumber, String reason) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new IllegalArgumentException("Quantidade de entrada deve ser positiva.");
        }
        Product p = productRepository.findByUuidForUpdate(productUuid)
                .orElseThrow(() -> new IllegalStateException(
                        "Produto " + productUuid + " não encontrado para entrada de estoque."));
        BigDecimal before = p.getStockQuantity();
        BigDecimal after = before.add(quantity);
        p.setStockQuantity(after);
        productRepository.save(p);

        StockMovement movement = new StockMovement(
                productUuid,
                quantity,
                before,
                after,
                MovementType.ENTRADA,
                source,
                sourceUuid,
                sourceNumber,
                reason);
        return stockMovementRepository.save(movement);
    }

    // ---------------------------------------------------------------------
    // Estorno por origem
    // ---------------------------------------------------------------------

    /**
     * Estorna todas as baixas (saídas) ainda não estornadas associadas a
     * um documento de origem. Para cada saída encontrada:
     * <ol>
     *   <li>recarrega o produto (com lock); se não existir mais, pula
     *       silenciosamente — o histórico é preservado mas o saldo não
     *       é restaurado, para não bloquear cancelamentos antigos;</li>
     *   <li>soma a quantidade devolvida ao saldo atual e salva o produto;</li>
     *   <li>cria uma movimentação complementar
     *       {@link MovementType#ESTORNO_SAIDA} referenciando a original
     *       via {@code reversalOfUuid};</li>
     *   <li>marca a original como {@code reversed=true}.</li>
     * </ol>
     * Idempotente: chamadas repetidas só estornam o que ainda não foi
     * estornado (filtragem por {@code reversed=false}).
     */
    @Transactional
    public void estornarSaidasPorOrigem(UUID sourceUuid, MovementSource source, String reason) {
        List<StockMovement> originals = stockMovementRepository
                .findBySourceUuidAndSourceOrderByCreatedAtAsc(sourceUuid, source);

        for (StockMovement original : originals) {
            if (original.isReversed() || original.getType() != MovementType.SAIDA) {
                continue;
            }
            // quantidade da saída original era negativa; inverte para devolver.
            BigDecimal devolution = original.getQuantityChange().negate();

            Product p = productRepository.findByUuidForUpdate(original.getProductUuid())
                    .orElse(null);
            if (p == null) {
                // Produto foi removido/inativado sem saldo: preserva o histórico,
                // não bloqueia o cancelamento. A movimentação de estorno é
                // registrada mesmo assim para auditoria.
                StockMovement reversal = new StockMovement(
                        original.getProductUuid(),
                        devolution,
                        original.getStockAfter(),
                        original.getStockAfter(),
                        MovementType.ESTORNO_SAIDA,
                        source,
                        sourceUuid,
                        original.getSourceNumber(),
                        reason);
                reversal.setReversalOfUuid(original.getUuid());
                stockMovementRepository.save(reversal);
                original.setReversed(true);
                stockMovementRepository.save(original);
                continue;
            }

            BigDecimal before = p.getStockQuantity();
            BigDecimal after = before.add(devolution);
            p.setStockQuantity(after);
            productRepository.save(p);

            StockMovement reversal = new StockMovement(
                    original.getProductUuid(),
                    devolution,
                    before,
                    after,
                    MovementType.ESTORNO_SAIDA,
                    source,
                    sourceUuid,
                    original.getSourceNumber(),
                    reason);
            reversal.setReversalOfUuid(original.getUuid());
            stockMovementRepository.save(reversal);

            original.setReversed(true);
            stockMovementRepository.save(original);
        }
    }

    // ---------------------------------------------------------------------
    // Consultas de apoio (usadas pelo controller)
    // ---------------------------------------------------------------------

    /**
     * Já existem saídas primárias (não estornadas) para a origem dada?
     * Usado para idempotência no avanço de status do pedido — evita
     * baixar duas vezes o mesmo pedido em retomadas/duas chamadas.
     */
    @Transactional(readOnly = true)
    public boolean existeSaidaNaoEstornada(UUID sourceUuid, MovementSource source) {
        return stockMovementRepository
                .existsBySourceUuidAndSourceAndTypeAndReversedFalse(
                        sourceUuid, source, MovementType.SAIDA);
    }

    /**
     * Item de um lote de saída. Record interno para clareza no
     * {@link #registrarSaidaEmLote}; o chamador monta a lista a partir
     * das linhas do documento de origem.
     */
    public record SaidaItem(UUID productUuid, BigDecimal quantity) {
    }

    // ---------------------------------------------------------------------
    // Leitura (controller)
    // ---------------------------------------------------------------------

    /**
     * Histórico paginado de um produto (mais recente primeiro). Resolve
     * nome/código do produto uma vez (snapshot momentâneo) e reaplica
     * para todas as movimentações da página.
     */
    @Transactional(readOnly = true)
    public PagedResponse<StockMovementResponse> historicoPorProduto(UUID productUuid, Pageable pageable) {
        Page<StockMovement> page = stockMovementRepository
                .findByProductUuidOrderByCreatedAtDesc(productUuid, pageable);
        Product snapshot = productRepository.findById(productUuid).orElse(null);
        Page<StockMovementResponse> mapped = page.map(m -> StockMovementMapper.toResponse(m, snapshot));
        return PagedResponse.from(mapped);
    }

    /**
     * Detalhe de uma movimentação. Resolve nome/código do produto no
     * momento da consulta.
     */
    @Transactional(readOnly = true)
    public StockMovementResponse movimentacaoPorId(UUID movementUuid) {
        StockMovement m = stockMovementRepository.findById(movementUuid)
                .orElseThrow(() -> new StockMovementNotFoundException(movementUuid));
        Product p = productRepository.findById(m.getProductUuid()).orElse(null);
        return StockMovementMapper.toResponse(m, p);
    }
}