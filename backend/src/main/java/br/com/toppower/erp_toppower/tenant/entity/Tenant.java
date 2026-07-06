package br.com.toppower.erp_toppower.tenant.entity;

import br.com.toppower.erp_toppower.common.annotation.UpperCase;
import br.com.toppower.erp_toppower.common.embeddable.Address;
import br.com.toppower.erp_toppower.common.entity.BaseEntity;
import br.com.toppower.erp_toppower.common.enums.RegistrationStatus;
import br.com.toppower.erp_toppower.common.validation.ValidCnpj;
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
 * Entidade que representa um <b>tenant</b> — a empresa (pessoa jurídica)
 * <b>dona</b> dos dados, identificada por CNPJ próprio. Cada tenant tem seus
 * próprios cadastros (empresas parceiras, clientes, propostas, etc.),
 * isolados dos demais tenants.
 *
 * <p>Não confundir com {@code Company}: {@code Company} é uma empresa
 * <b>cliente</b> do ERP (pessoa jurídica para quem se emite proposta/nota),
 * enquanto {@code Tenant} é a empresa que <b>opera</b> o ERP.</p>
 *
 * <p><b>Não herda de {@code TenantScopedEntity}</b> — o próprio tenant não
 * tem um {@code tenant_uuid} (ele é o tenant). Herda diretamente de
 * {@link BaseEntity} para UUID + auditoria.</p>
 */
@Entity
@Table(name = "tenants")
@Getter
@Setter
@NoArgsConstructor
public class Tenant extends BaseEntity {

    /**
     * Razão social — nome oficial/registrado da empresa operadora.
     * Obrigatório. Salvo em MAIÚSCULAS (UpperCaseFieldListener).
     */
    @UpperCase
    @Column(name = "legal_name", nullable = false, length = 200)
    private String legalName;

    /**
     * Nome fantasia — nome comercial da empresa operadora.
     * Opcional. Salvo em MAIÚSCULAS (UpperCaseFieldListener).
     */
    @UpperCase
    @Column(name = "trade_name", length = 200)
    private String tradeName;

    /**
     * Código interno único do tenant (ex.: {@code "TEN000001"}).
     * Gerado automaticamente pelo {@code TenantService} no cadastro.
     * Imutável após o cadastro.
     */
    @Column(name = "code", unique = true, nullable = false, updatable = false, length = 50)
    private String code;

    /**
     * CNPJ do tenant (14 dígitos, com ou sem formatação).
     * Validado por {@link ValidCnpj}. Obrigatório, único e imutável.
     */
    @ValidCnpj(message = "CNPJ inválido (dígitos verificadores incorretos ou formato incorreto)")
    @Column(name = "cnpj", nullable = false, length = 20, unique = true, updatable = false)
    private String cnpj;

    /**
     * Inscrição Estadual (IE) — registro da empresa na Secretaria da Fazenda.
     * Opcional.
     */
    @Column(name = "state_registration", length = 30)
    private String stateRegistration;

    /**
     * Indica se a empresa é ISENTA de Inscrição Estadual (IE Isento).
     * Quando {@code true}, a empresa é dispensada de possuir IE — caso
     * comum de MEIs, prestadores de serviço e empresas em início de
     * atividade.
     *
     * <p>Default: {@code false}.</p>
     */
    @Column(name = "state_registration_exempt", nullable = false)
    private boolean stateRegistrationExempt;

    /**
     * Inscrição Municipal (IM) — registro da empresa na Prefeitura.
     * Opcional.
     */
    @Column(name = "municipal_registration", length = 30)
    private String municipalRegistration;

    /**
     * Endereço da empresa operadora. Os campos do {@link Address} são embutidos
     * nesta tabela com o prefixo {@code address_} para evitar conflito
     * com colunas próprias da entidade.
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
    private RegistrationStatus status;

    /**
     * Inicialização antes de persistir: garante status ATIVO quando não informado.
     */
    @PrePersist
    private void onPrePersist() {
        if (status == null) {
            status = RegistrationStatus.ATIVO;
        }
    }
}