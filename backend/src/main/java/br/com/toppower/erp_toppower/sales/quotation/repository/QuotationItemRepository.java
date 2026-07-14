package br.com.toppower.erp_toppower.sales.quotation.repository;

import br.com.toppower.erp_toppower.sales.quotation.entity.QuotationItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuotationItemRepository extends JpaRepository<QuotationItem, Long> {

    /**
     * Retorna todos os itens de uma proposta, ordenados pela data de
     * criação (primeiro item inserido primeiro).
     */
    List<QuotationItem> findByQuotationIdOrderByCreatedAtAsc(Long quotationId);

    /**
     * Remove todos os itens de uma proposta. Usado em substituição
     * completa da lista de itens (update com delta de linhas).
     */
    void deleteByQuotationId(Long quotationId);

    /**
     * Quantidade de itens de uma proposta.
     */
    long countByQuotationId(Long quotationId);
}
