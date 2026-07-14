package br.com.toppower.erp_toppower.sales.salesorder.repository;

import br.com.toppower.erp_toppower.sales.salesorder.entity.SalesOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalesOrderItemRepository extends JpaRepository<SalesOrderItem, Long> {

    /**
     * Retorna todos os itens de um pedido, ordenados pela data de
     * criação (primeiro item inserido primeiro).
     */
    List<SalesOrderItem> findBySalesOrderIdOrderByCreatedAtAsc(Long salesOrderId);

    /**
     * Remove todos os itens de um pedido. Usado em substituição
     * completa da lista de itens (update com delta de linhas).
     */
    void deleteBySalesOrderId(Long salesOrderId);

    /**
     * Quantidade de itens de um pedido.
     */
    long countBySalesOrderId(Long salesOrderId);
}