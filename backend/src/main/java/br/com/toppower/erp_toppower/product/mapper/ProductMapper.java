package br.com.toppower.erp_toppower.product.mapper;

import br.com.toppower.erp_toppower.product.dto.ProductCreateRequest;
import br.com.toppower.erp_toppower.product.dto.ProductResponse;
import br.com.toppower.erp_toppower.product.dto.ProductUpdateRequest;
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
     *   <li>Os campos fiscais {@code origem}, {@code csosn}, {@code cstIpi},
     *   {@code cstPis} e {@code cstCofins} quando {@code null} recebem default
     *   no {@code @PrePersist} (Simples Nacional).</li>
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
        // Campos fiscais
        product.setNcm(request.ncm());
        product.setOrigem(request.origem());
        product.setCodigoBarras(request.codigoBarras());
        product.setCest(request.cest());
        product.setExTipi(request.exTipi());
        product.setPesoLiquido(request.pesoLiquido());
        product.setPesoBruto(request.pesoBruto());
        product.setCsosn(request.csosn());
        product.setAliquotaIcmsSt(request.aliquotaIcmsSt());
        product.setMvaSt(request.mvaSt());
        product.setCstIpi(request.cstIpi());
        product.setClasseEnqIpi(request.classeEnqIpi());
        product.setCstPis(request.cstPis());
        product.setCstCofins(request.cstCofins());
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
                product.getNcm(),
                product.getOrigem(),
                product.getCodigoBarras(),
                product.getCest(),
                product.getExTipi(),
                product.getPesoLiquido(),
                product.getPesoBruto(),
                product.getCsosn(),
                product.getAliquotaIcmsSt(),
                product.getMvaSt(),
                product.getCstIpi(),
                product.getClasseEnqIpi(),
                product.getCstPis(),
                product.getCstCofins(),
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
        // Campos fiscais
        if (request.ncm() != null) {
            product.setNcm(request.ncm());
        }
        if (request.origem() != null) {
            product.setOrigem(request.origem());
        }
        if (request.codigoBarras() != null) {
            product.setCodigoBarras(request.codigoBarras());
        }
        if (request.cest() != null) {
            product.setCest(request.cest());
        }
        if (request.exTipi() != null) {
            product.setExTipi(request.exTipi());
        }
        if (request.pesoLiquido() != null) {
            product.setPesoLiquido(request.pesoLiquido());
        }
        if (request.pesoBruto() != null) {
            product.setPesoBruto(request.pesoBruto());
        }
        if (request.csosn() != null) {
            product.setCsosn(request.csosn());
        }
        if (request.aliquotaIcmsSt() != null) {
            product.setAliquotaIcmsSt(request.aliquotaIcmsSt());
        }
        if (request.mvaSt() != null) {
            product.setMvaSt(request.mvaSt());
        }
        if (request.cstIpi() != null) {
            product.setCstIpi(request.cstIpi());
        }
        if (request.classeEnqIpi() != null) {
            product.setClasseEnqIpi(request.classeEnqIpi());
        }
        if (request.cstPis() != null) {
            product.setCstPis(request.cstPis());
        }
        if (request.cstCofins() != null) {
            product.setCstCofins(request.cstCofins());
        }
    }
}