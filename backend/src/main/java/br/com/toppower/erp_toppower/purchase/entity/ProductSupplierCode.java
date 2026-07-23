package br.com.toppower.erp_toppower.purchase.entity;

import br.com.toppower.erp_toppower.common.annotation.UpperCase;
import br.com.toppower.erp_toppower.common.entity.OrganizationScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Relação entre um produto cadastrado e o código que um fornecedor
 * específico utiliza para o mesmo item (campo {@code cProd} da NF-e).
 *
 * <p>Permite que importações futuras de XML do mesmo fornecedor casem
 * instantaneamente pelo código do produto no fornecedor, mesmo que o
 * SKU interno ({@code Product.code}) seja diferente. Funciona como o
 * "catálogo de códigos por fornecedor" usado por ERPs como o Bling.</p>
 *
 * <p><b>Organization-scoped</b>: herda de {@link OrganizationScopedEntity}
 * para garantir isolamento multi-tenant via {@code organizationFilter}.</p>
 */
@Entity
@Table(
        name = "product_supplier_codes",
        indexes = {
                @Index(name = "idx_psc_product", columnList = "product_id"),
                @Index(name = "idx_psc_supplier", columnList = "supplier_id")
        },
        uniqueConstraints = @UniqueConstraint(
                name = "uk_psc_org_supplier_code",
                columnNames = {"organization_id", "supplier_id", "supplier_code"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class ProductSupplierCode extends OrganizationScopedEntity {

    /**
     * Referência ao {@code Product} cadastrado. Obrigatório.
     */
    @Column(name = "product_id", nullable = false)
    private Long productId;

    /**
     * Referência ao {@code Supplier} (fornecedor) que utiliza o código.
     * Obrigatório.
     */
    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    /**
     * Código do produto no fornecedor (campo {@code cProd} da NF-e).
     * Salvo em MAIÚSCULAS.
     */
    @UpperCase
    @Column(name = "supplier_code", nullable = false, length = 60)
    private String supplierCode;
}