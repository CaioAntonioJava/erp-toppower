package br.com.toppower.erp_toppower.product.service;

import br.com.toppower.erp_toppower.common.dto.PagedResponse;
import br.com.toppower.erp_toppower.product.dto.ProductCreateRequest;
import br.com.toppower.erp_toppower.product.dto.ProductResponse;
import br.com.toppower.erp_toppower.product.dto.ProductUpdateRequest;
import br.com.toppower.erp_toppower.product.entity.Product;
import br.com.toppower.erp_toppower.product.enums.ProductStatus;
import br.com.toppower.erp_toppower.product.exception.DuplicateProductCodeException;
import br.com.toppower.erp_toppower.product.exception.ProductNotFoundException;
import br.com.toppower.erp_toppower.product.mapper.ProductMapper;
import br.com.toppower.erp_toppower.product.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ProductService {

    private static final int MIN_SEARCH_QUERY_LENGTH = 2;

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public ProductResponse create(ProductCreateRequest request) {
        if (productRepository.existsByCode(request.code())) {
            throw new DuplicateProductCodeException(request.code());
        }
        Product product = ProductMapper.toEntity(request);
        Product saved = productRepository.save(product);
        return ProductMapper.toResponse(saved);
    }

    /**
     * Lista paginada de produtos. Se {@code status} for nulo, retorna todos os produtos
     * (ativos e inativos); caso contrário filtra pelo status informado.
     */
    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> getAll(ProductStatus status, Pageable pageable) {
        Page<Product> page = (status == null)
                ? productRepository.findAll(pageable)
                : productRepository.findByStatus(status, pageable);
        Page<ProductResponse> mapped = page.map(ProductMapper::toResponse);
        return PagedResponse.from(mapped);
    }

    /**
     * Lista paginada apenas de produtos com status {@link ProductStatus#ATIVO}.
     * Atalho semântico para {@code getAll(ProductStatus.ATIVO, pageable)}.
     */
    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> findActive(Pageable pageable) {
        return getAll(ProductStatus.ATIVO, pageable);
    }

    /**
     * Lista paginada apenas de produtos com status {@link ProductStatus#INATIVO}.
     * Atalho semântico para {@code getAll(ProductStatus.INATIVO, pageable)}.
     * Útil para relatórios de produtos a serem reativados ou removidos.
     */
    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> findInactive(Pageable pageable) {
        return getAll(ProductStatus.INATIVO, pageable);
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(UUID id) {
        return productRepository.findById(id)
                .map(ProductMapper::toResponse)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Transactional
    public ProductResponse update(UUID id, ProductUpdateRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        if (request.code() != null && !request.code().equals(product.getCode())) {
            if (productRepository.existsByCode(request.code())) {
                throw new DuplicateProductCodeException(request.code());
            }
        }

        ProductMapper.applyUpdate(product, request);
        Product saved = productRepository.save(product);
        return ProductMapper.toResponse(saved);
    }

    /**
     * Soft delete: não remove fisicamente o registro, apenas altera o status para INATIVO.
     * Preserva o histórico em pedidos/notas fiscais e a rastreabilidade do registro.
     */
    @Transactional
    public void softDelete(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        product.setStatus(ProductStatus.INATIVO);
        productRepository.save(product);
    }

    /**
     * Busca case-insensitive por substring em {@code name} ou {@code code},
     * com filtro opcional de status. Quando {@code status} é {@code null},
     * retorna ATIVOS e INATIVOS. Quando informado, filtra.
     */
    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> search(String query, ProductStatus status, Pageable pageable) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("O termo de busca é obrigatório");
        }
        String trimmed = query.trim();
        if (trimmed.length() < MIN_SEARCH_QUERY_LENGTH) {
            throw new IllegalArgumentException(
                    "O termo de busca deve ter ao menos " + MIN_SEARCH_QUERY_LENGTH + " caracteres");
        }
        Page<ProductResponse> mapped = productRepository
                .searchByQuery(status, trimmed, pageable)
                .map(ProductMapper::toResponse);
        return PagedResponse.from(mapped);
    }
}
