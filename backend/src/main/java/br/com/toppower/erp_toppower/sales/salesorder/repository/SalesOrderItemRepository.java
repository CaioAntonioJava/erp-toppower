package br.com.toppower.erp_toppower.sales.salesorder.repository;

import br.com.toppower.erp_toppower.sales.salesorder.entity.SalesOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SalesOrderItemRepository extends JpaRepository<SalesOrderItem, UUID> {

    /**
     * Retorna todos os itens de um pedido, ordenados pela data de
     * criação (primeiro item inserido primeiro).
     */
    List<SalesOrderItem> findBySalesOrderUuidOrderByCreatedAtAsc(UUID salesOrderUuid);

    /**
     * Remove todos os itens de um pedido. Usado em substituição
     * completa da lista de itens (update com delta de linhas).
     */
    void deleteBySalesOrderUuid(UUID salesOrderUuid);

    /**
     * Quantidade de itens de um pedido.
     */
    long countBySalesOrderUuid(UUID salesOrderUuid);
}