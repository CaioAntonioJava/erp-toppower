package br.com.toppower.erp_toppower.sales.salesorder.repository;

import br.com.toppower.erp_toppower.sales.salesorder.entity.SalesOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SalesOrderRepository extends JpaRepository<SalesOrder, UUID>,
        JpaSpecificationExecutor<SalesOrder> {

    boolean existsByNumber(Long number);

    Optional<SalesOrder> findByNumber(Long number);

    /**
     * Retorna o maior número de pedido já emitido. Usado para gerar o
     * próximo número sequencial (a partir de {@code 1000} no primeiro
     * pedido).
     *
     * <p>Retorna {@code null} quando ainda não houver nenhum pedido
     * cadastrado. Nesse caso, o serviço usa {@code 1000} como ponto de
     * partida.</p>
     */
    @Query("SELECT MAX(o.number) FROM SalesOrder o")
    Long findMaxNumber();
}