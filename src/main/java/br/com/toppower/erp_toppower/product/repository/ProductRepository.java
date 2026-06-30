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
     * Busca flexível por texto (opcional) e/ou status (opcional).
     * <ul>
     *   <li>{@code query} nulo/blank → ignora o filtro de texto</li>
     *   <li>{@code status} nulo → ignora o filtro de status</li>
     *   <li>Ambos nulos → retorna todos os produtos (paginado)</li>
     * </ul>
     * Quando {@code query} é informado, busca case-insensitive em {@code name} ou {@code code}.
     */
    @Query("""
            SELECT p FROM Product p
            WHERE (:status IS NULL OR p.status = :status)
              AND (:query IS NULL
                OR LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(p.code) LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    Page<Product> searchByQuery(@Param("status") ProductStatus status,
                                @Param("query") String query,
                                Pageable pageable);
}
