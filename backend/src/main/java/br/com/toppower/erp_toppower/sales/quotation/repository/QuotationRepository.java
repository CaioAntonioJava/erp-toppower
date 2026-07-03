package br.com.toppower.erp_toppower.sales.quotation.repository;

import br.com.toppower.erp_toppower.sales.quotation.entity.Quotation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuotationRepository extends JpaRepository<Quotation, UUID>,
        JpaSpecificationExecutor<Quotation> {

    boolean existsByNumber(Long number);

    Optional<Quotation> findByNumber(Long number);

    /**
     * Retorna o maior número de proposta já emitido. Usado para gerar o
     * próximo número sequencial (a partir de {@code 1500} na primeira
     * proposta).
     *
     * <p>Retorna {@code null} quando ainda não houver nenhuma proposta
     * cadastrada. Nesse caso, o serviço usa {@code 1500} como ponto de
     * partida.</p>
     */
    @Query("SELECT MAX(q.number) FROM Quotation q")
    Long findMaxNumber();

    /**
     * Busca paginada por status (opcional).
     * Se {@code status} for nulo, retorna todas as propostas.
     */
    Page<Quotation> findByStatus(br.com.toppower.erp_toppower.sales.quotation.enums.QuotationStatus status,
                                 Pageable pageable);
}
