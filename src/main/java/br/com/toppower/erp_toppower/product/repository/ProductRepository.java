package br.com.toppower.erp_toppower.product.repository;

import br.com.toppower.erp_toppower.product.entity.Product;
import br.com.toppower.erp_toppower.product.enums.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    boolean existsByCode(String code);

    Optional<Product> findByCode(String code);

    /**
     * Busca case-insensitive por substring em {@code name} OU {@code code},
     * filtrando apenas produtos com o {@code status} informado e ordenando pelo nome.
     * Tipico para campo de busca de uma loja virtual.
     */
    @Query("""
            SELECT p FROM Product p
            WHERE p.status = :status
              AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(p.code) LIKE LOWER(CONCAT('%', :query, '%')))
            ORDER BY p.name ASC
            """)
    List<Product> searchByQuery(@Param("status") ProductStatus status,
                                @Param("query") String query);
}
