package br.com.toppower.erp_toppower.product.mapper;

import br.com.toppower.erp_toppower.product.dto.ProductCreateRequest;
import br.com.toppower.erp_toppower.product.dto.ProductUpdateRequest;
import br.com.toppower.erp_toppower.product.dto.ProductResponse;
import br.com.toppower.erp_toppower.product.entity.Product;

public final class ProductMapper {

    private ProductMapper() {
    }

    /**
     * Cria uma nova entidade a partir do request de criação.
     * Observações:
     * <ul>
     *   <li>O {@code code} (SKU) é <b>opcional</b> — pode ser {@code null}.</li>
     *   <li>O {@code status} pode ser {@code null}; o {@code @PrePersist}
     *   da entidade cuida de aplicar o default {@code ATIVO}.</li>
     * </ul>
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
                product.getId(),
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
     * Aplica uma atualização parcial (PATCH) na entidade carregada.
     * Apenas campos não-nulos do request sobrescrevem o estado atual.
     * <p>O {@code code} (SKU) aceita {@code null} na atualização apenas se for
     * enviado explicitamente — a checagem de duplicidade no service ignora
     * valores em branco.</p>
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
