package br.com.toppower.erp_toppower.supplier.entity;

import br.com.toppower.erp_toppower.common.annotation.UpperCase;
import br.com.toppower.erp_toppower.common.embeddable.Address;
import br.com.toppower.erp_toppower.common.entity.OrganizationScopedEntity;
import br.com.toppower.erp_toppower.common.validation.ValidCnpj;
import br.com.toppower.erp_toppower.supplier.enums.SupplierStatus;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidade que representa um fornecedor do sistema.
 *
 * <p>Herdar {@link BaseEntity} fornece o identificador UUID e a auditoria
 * completa (createdAt, updatedAt, createdBy, updatedBy).</p>
 *
 * <p>Fornecedores são tipicamente pessoas jurídicas (CNPJ). O campo
 * {@code taxId} é validado por {@link ValidCnpj} (incluindo dígitos
 * verificadores).</p>
 */
@Entity
@Table(name = "suppliers")
@Getter
@Setter
@NoArgsConstructor
public class Supplier extends OrganizationScopedEntity {

    /**
     * Razão social — nome oficial/registrado do fornecedor.
     * Obrigatório. Salvo em MAIÚSCULAS (UpperCaseFieldListener).
     */
    @UpperCase
    @Column(name = "legal_name", nullable = false, length = 200)
    private String legalName;

    /**
     * Nome fantasia — nome comercial do fornecedor.
     * Opcional. Salvo em MAIÚSCULAS (UpperCaseFieldListener).
     */
    @UpperCase
    @Column(name = "trade_name", length = 200)
    private String tradeName;

    /**
     * CNPJ (14 dígitos com dígitos verificadores).
     * Obrigatório e único. Validado por {@link ValidCnpj}.
     */
    @ValidCnpj(message = "CNPJ inválido (dígitos verificadores incorretos ou formato incorreto)")
    // unique = true removido: a unicidade agora é por Organization
    // (uk_suppliers_org_tax_id em V22). O Hibernate ddl-auto=update criaria
    // um índice UNIQUE global que conflitaria com a constraint composta.
    @Column(name = "tax_id", nullable = false, length = 20)
    private String taxId;

    /**
     * Inscrição Estadual (IE) — registro na Secretaria da Fazenda.
     * Opcional.
     */
    @Column(name = "state_registration", length = 30)
    private String stateRegistration;

    /**
     * Inscrição Municipal (IM) — registro na Prefeitura.
     * Opcional.
     */
    @Column(name = "municipal_registration", length = 30)
    private String municipalRegistration;

    /**
     * E-mail de contato principal.
     * Obrigatório.
     */
    @Column(name = "email", nullable = false, length = 150)
    private String email;

    /**
     * Telefone de contato principal.
     * Opcional.
     */
    @Column(name = "phone", length = 20)
    private String phone;

    /**
     * Nome da pessoa de contato no fornecedor (ex: "João - Vendas").
     * Opcional. Salvo em MAIÚSCULAS (UpperCaseFieldListener).
     */
    @UpperCase
    @Column(name = "contact_name", length = 150)
    private String contactName;

    /**
     * Endereço do fornecedor (logradouro, número, cidade, UF, CEP, etc.).
     * <p>Os campos do {@link Address} são embutidos nesta mesma tabela
     * ({@code suppliers}) com o prefixo {@code address_} nas colunas.</p>
     */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "street", column = @Column(name = "address_street", nullable = false, length = 200)),
            @AttributeOverride(name = "number", column = @Column(name = "address_number", nullable = false, length = 20)),
            @AttributeOverride(name = "complement", column = @Column(name = "address_complement", length = 100)),
            @AttributeOverride(name = "neighborhood", column = @Column(name = "address_neighborhood", length = 100)),
            @AttributeOverride(name = "city", column = @Column(name = "address_city", nullable = false, length = 100)),
            @AttributeOverride(name = "state", column = @Column(name = "address_state", nullable = false, length = 2)),
            @AttributeOverride(name = "zipCode", column = @Column(name = "address_zip_code", nullable = false, length = 9))
    })
    private Address address;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SupplierStatus status;

    /**
     * Inicialização do fornecedor antes de persistir.
     * Garante que o status seja {@link SupplierStatus#ATIVO} quando não for informado.
     */
    @PrePersist
    private void onPrePersist() {
        if (status == null) {
            status = SupplierStatus.ATIVO;
        }
    }
}
