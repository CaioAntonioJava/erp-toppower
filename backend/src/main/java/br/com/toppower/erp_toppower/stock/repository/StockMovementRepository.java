package br.com.toppower.erp_toppower.stock.repository;

import br.com.toppower.erp_toppower.stock.entity.StockMovement;
import br.com.toppower.erp_toppower.stock.enums.MovementSource;
import br.com.toppower.erp_toppower.stock.enums.MovementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {

    /**
     * Todas as movimentações de um documento de origem, em ordem
     * cronológica. Usado no estorno para iterar e inverter cada baixa.
     */
    List<StockMovement> findBySourceUuidAndSourceOrderByCreatedAtAsc(UUID sourceUuid,
                                                                    MovementSource source);

    /**
     * Histórico de um produto (mais recente primeiro), paginado.
     */
    Page<StockMovement> findByProductUuidOrderByCreatedAtDesc(UUID productUuid, Pageable pageable);

    /**
     * Verifica se já existe movimentação primária (não estornada) de um
     * tipo para a origem dada. Usado para idempotência no avanço de
     * status do pedido (não baixar duas vezes o mesmo pedido).
     */
    boolean existsBySourceUuidAndSourceAndTypeAndReversedFalse(UUID sourceUuid,
                                                               MovementSource source,
                                                               MovementType type);
}