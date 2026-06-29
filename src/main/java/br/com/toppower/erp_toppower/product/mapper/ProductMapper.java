package br.com.toppower.erp_toppower.product.mapper;

import br.com.toppower.erp_toppower.product.dto.ProductCreateRequest;
import br.com.toppower.erp_toppower.product.dto.ProductUpdateRequest;
import br.com.toppower.erp_toppower.product.dto.ProductResponse;
import br.com.toppower.erp_toppower.product.entity.Product;

public final class ProductMapper {

    private ProductMapper() {
    }

    /**
     * Cria uma nova entidade a partir do request de criacao.
     * Observacao: o {@code status} pode ser {@code null}; o {@code @PrePersist}
     * da entidade cuida de aplicar o default {@code ATIVO}.
     */
    public static Product toEntity(ProductCreateRequest request) {
        Product product = new Product();
        product.setName(request.name());
        product.setCode(request.code());
        product.setUnitType(request.unitType());
        product.setStatus(request.status());
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity());
        return product;
    }

    public static ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getUuid(),
                product.getName(),
                product.getCode(),
                product.getUnitType(),
                product.getStatus(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getCreatedAt(),
                product.getUpdatedAt(),
                product.getCreatedBy(),
                product.getUpdatedBy()
        );
    }

    /**
     * Aplica uma atualizacao parcial (PATCH) na entidade carregada.
     * Apenas campos nao-nulos do request sobrescrevem o estado atual.
     */
    public static void applyUpdate(Product product, ProductUpdateRequest request) {
        if (request.name() != null) {
            product.setName(request.name());
        }
        if (request.code() != null) {
            product.setCode(request.code());
        }
        if (request.unitType() != null) {
            product.setUnitType(request.unitType());
        }
        if (request.status() != null) {
            product.setStatus(request.status());
        }
        if (request.price() != null) {
            product.setPrice(request.price());
        }
        if (request.stockQuantity() != null) {
            product.setStockQuantity(request.stockQuantity());
        }
    }
}
