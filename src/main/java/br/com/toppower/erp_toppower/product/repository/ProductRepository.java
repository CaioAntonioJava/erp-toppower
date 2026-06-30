package br.com.toppower.erp_toppower.product.repository;

import br.com.toppower.erp_toppower.product.entity.Product;
import br.com.toppower.erp_toppower.product.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    boolean existsByCode(String code);

    Optional<Product> findByCode(String code);

    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    /**
     * Busca case-insensitive por substring em {@code name} OU {@code code}.
     * Retorna produtos ATIVOS e INATIVOS (sem filtro de status), ordenados pelo nome.
     * Paginado para suportar listagens grandes.
     */
    @Query("""
            SELECT p FROM Product p
            WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(p.code) LIKE LOWER(CONCAT('%', :query, '%'))
            """)
    Page<Product> searchByQuery(@Param("query") String query, Pageable pageable);
}
