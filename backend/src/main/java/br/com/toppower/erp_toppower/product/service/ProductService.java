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

@Service
public class ProductService {

    private static final int MIN_SEARCH_QUERY_LENGTH = 2;

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public ProductResponse create(ProductCreateRequest request) {
        // code (SKU) é opcional: só validamos duplicidade quando foi informado.
        if (request.code() != null && !request.code().isBlank()
                && productRepository.existsByCode(request.code())) {
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
    public ProductResponse getById(Long id) {
        return productRepository.findById(id)
                .map(ProductMapper::toResponse)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Transactional
    public ProductResponse update(Long id, ProductUpdateRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        if (request.code() != null && !request.code().isBlank()
                && !request.code().equals(product.getCode())
                && productRepository.existsByCode(request.code())) {
            throw new DuplicateProductCodeException(request.code());
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
    public void softDelete(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        product.setStatus(ProductStatus.INATIVO);
        productRepository.save(product);
    }

    /**
     * Busca flexível por texto (opcional) e/ou status (opcional).
     * <ul>
     *   <li>Apenas {@code status} → lista todos os produtos com aquele status</li>
     *   <li>Apenas {@code query} → lista todos os produtos que dão match com o texto</li>
     *   <li>Ambos → lista os produtos com aquele status E que dão match com o texto</li>
     *   <li>Nenhum → lista todos os produtos (paginado)</li>
     * </ul>
     * Quando {@code query} é informado, exige no mínimo 2 caracteres.
     */
    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> search(String query, ProductStatus status, Pageable pageable) {
        String trimmed = (query == null) ? null : query.trim();
        if (trimmed != null && !trimmed.isEmpty() && trimmed.length() < MIN_SEARCH_QUERY_LENGTH) {
            throw new IllegalArgumentException(
                    "O termo de busca deve ter ao menos " + MIN_SEARCH_QUERY_LENGTH + " caracteres");
        }
        Page<ProductResponse> mapped = productRepository
                .searchByQuery(status, trimmed, pageable)
                .map(ProductMapper::toResponse);
        return PagedResponse.from(mapped);
    }
}
