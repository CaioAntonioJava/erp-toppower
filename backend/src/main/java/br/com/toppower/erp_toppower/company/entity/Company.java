package br.com.toppower.erp_toppower.company.entity;

import br.com.toppower.erp_toppower.common.annotation.UpperCase;
import br.com.toppower.erp_toppower.common.embeddable.Address;
import br.com.toppower.erp_toppower.common.entity.OrganizationScopedEntity;
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
 * Entidade que representa uma empresa (pessoa jurídica) cliente do sistema.
 *
 * <p>Herdar {@link BaseEntity} fornece o identificador UUID e a auditoria
 * completa (createdAt, updatedAt, createdBy, updatedBy).</p>
 *
 * <p>O tipo de pessoa é implicitamente JURÍDICA (CNPJ). Para pessoa física,
 * use a entidade {@code Customer}.</p>
 */
@Entity
@Table(name = "companies")
@Getter
@Setter
@NoArgsConstructor
public class Company extends OrganizationScopedEntity {

    /**
     * Razão social — nome oficial/registrado da empresa.
     * Obrigatório. Salvo em MAIÚSCULAS (UpperCaseFieldListener).
     */
    @UpperCase
    @Column(name = "legal_name", nullable = false, length = 200)
    private String legalName;

    /**
     * Nome fantasia — nome comercial da empresa.
     * Opcional. Salvo em MAIÚSCULAS (UpperCaseFieldListener).
     */
    @UpperCase
    @Column(name = "trade_name", length = 200)
    private String tradeName;

    /**
     * Código interno único da empresa (ex.: {@code "EMP000001"}, {@code "EMP000002"}).
     * Gerado automaticamente pelo {@code CompanyService} no momento do cadastro
     * a partir do prefixo {@code EMP} + sequência de 6 dígitos. Imutável após
     * o cadastro (nunca é alterado pelo {@code CompanyUpdateRequest}).
     */
    @Column(name = "code", unique = true, nullable = false, updatable = false, length = 50)
    private String code;

    /**
     * CNPJ (14 dígitos, com ou sem formatação).
     * Validado por {@link ValidCnpj} (incluindo dígitos verificadores).
     * Obrigatório e único. Imutável após o cadastro.
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
     * Quando {@code true}, significa que a empresa é dispensada de possuir
     * IE — caso comum de MEIs, prestadores de serviço e empresas em início
     * de atividade. Nesse cenário, o sistema não deve exigir/exibir a IE
     * em documentos fiscais.
     *
     * <p>Default: {@code false} (empresa NÃO é isenta).</p>
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
     * Endereço da empresa. Os campos do {@link Address} são embutidos
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
     * Inicialização antes de persistir: garante que o status seja
     * {@link RegistrationStatus#ATIVO} quando não for informado.
     * Não sobrescreve valores já definidos pelo chamador.
     */
    @PrePersist
    private void onPrePersist() {
        if (status == null) {
            status = RegistrationStatus.ATIVO;
        }
    }
}
