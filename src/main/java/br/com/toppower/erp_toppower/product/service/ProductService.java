package br.com.toppower.erp_toppower.product.service;

import br.com.toppower.erp_toppower.product.dto.ProductCreateRequest;
import br.com.toppower.erp_toppower.product.dto.ProductResponse;
import br.com.toppower.erp_toppower.product.dto.ProductUpdateRequest;
import br.com.toppower.erp_toppower.product.entity.Product;
import br.com.toppower.erp_toppower.product.enums.ProductStatus;
import br.com.toppower.erp_toppower.product.exception.DuplicateProductCodeException;
import br.com.toppower.erp_toppower.product.exception.ProductNotFoundException;
import br.com.toppower.erp_toppower.product.mapper.ProductMapper;
import br.com.toppower.erp_toppower.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

    @Transactional(readOnly = true)
    public List<ProductResponse> getAll() {
        return productRepository.findAll().stream()
                .map(ProductMapper::toResponse)
                .toList();
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
     * Soft delete: nao remove fisicamente o registro, apenas altera o status para INATIVO.
     * Preserva o historico em pedidos/notas fiscais e a rastreabilidade do registro.
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
     * retornando apenas produtos com status {@link ProductStatus#ATIVO}.
     * Pensado para alimentar o campo de busca de uma loja virtual.
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> search(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("O termo de busca eh obrigatorio");
        }
        String trimmed = query.trim();
        if (trimmed.length() < MIN_SEARCH_QUERY_LENGTH) {
            throw new IllegalArgumentException(
                    "O termo de busca deve ter ao menos " + MIN_SEARCH_QUERY_LENGTH + " caracteres");
        }
        return productRepository.searchByQuery(ProductStatus.ATIVO, trimmed).stream()
                .map(ProductMapper::toResponse)
                .toList();
    }
}
