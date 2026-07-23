package br.com.toppower.erp_toppower.purchase.repository;

import br.com.toppower.erp_toppower.purchase.entity.ProductSupplierCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductSupplierCodeRepository extends JpaRepository<ProductSupplierCode, Long> {

    /**
     * Busca a relação de código por fornecedor — usado no matching de
     * produtos na importação de NF-e (primeiro critério de similaridade).
     */
    Optional<ProductSupplierCode> findBySupplierIdAndSupplierCode(Long supplierId, String supplierCode);

    /**
     * Verifica se já existe relação para o par fornecedor/código.
     */
    boolean existsBySupplierIdAndSupplierCode(Long supplierId, String supplierCode);

    /**
     * Todas as relações de um produto (para auditoria/visualização).
     */
    List<ProductSupplierCode> findByProductId(Long productId);
}