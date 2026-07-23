package br.com.toppower.erp_toppower.product.repository;

import br.com.toppower.erp_toppower.product.entity.Product;
import br.com.toppower.erp_toppower.product.enums.ProductStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsByCode(String code);

    Optional<Product> findByCode(String code);

    Optional<Product> findByCodigoBarras(String codigoBarras);

    /**
     * Todos os produtos ativos com o NCM informado. Usado como candidatos
     * no matching por similaridade de nome na importação de NF-e — o
     * pré-filtro por NCM reduz o universo de comparação e falsos positivos.
     */
    List<Product> findByStatusAndNcm(ProductStatus status, String ncm);

    /**
     * Todos os produtos ativos. Usado como fallback no matching por
     * similaridade de nome quando o NCM não está disponível.
     */
    List<Product> findByStatus(ProductStatus status);

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

    /**
     * Carrega um produto aplicando lock pessimista de escrita
     * ({@code PESSIMISTIC_WRITE}). Usado pelo {@code StockService} antes de
     * alterar o saldo, serializando vendas simultâneas do mesmo produto e
     * evitando conditions de race nos testes do saldo.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);
}
