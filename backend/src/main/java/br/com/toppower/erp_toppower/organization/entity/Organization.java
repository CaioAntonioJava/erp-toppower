package br.com.toppower.erp_toppower.organization.entity;

import br.com.toppower.erp_toppower.common.annotation.UpperCase;
import br.com.toppower.erp_toppower.common.entity.BaseEntity;
import br.com.toppower.erp_toppower.common.validation.ValidCnpj;
import br.com.toppower.erp_toppower.organization.enums.OrganizationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * Entidade que representa uma empresa (Organization) cadastrada no ERP.
 *
 * <p>Cada Organization corresponde a um CNPJ próprio e possui seu próprio
 * conjunto isolado de dados de negócio (clientes, produtos, pedidos, etc.).
 * Um usuário pode ter acesso a uma ou mais Organizations, alternando entre
 * elas via header {@code X-Organization-Id} sem necessidade de novo login.</p>
 *
 * <p><b>Global</b>: não herda de {@code OrganizationScopedEntity} — a própria
 * Organization é a raiz do isolamento e não pertence a outra empresa.</p>
 */
@Entity
@Table(name = "organizations")
@Getter
@Setter
@NoArgsConstructor
public class Organization extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @UpperCase
    @Column(name = "corporate_name", nullable = false, length = 200)
    private String corporateName;

    @UpperCase
    @Column(name = "trade_name", nullable = false, length = 200)
    private String tradeName;

    @ValidCnpj
    @Column(name = "cnpj", nullable = false, unique = true, length = 18)
    private String cnpj;

    @UpperCase
    @Column(name = "state_registration", length = 50)
    private String stateRegistration;

    @UpperCase
    @Column(name = "municipal_registration", length = 50)
    private String municipalRegistration;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "zip_code", length = 9)
    private String zipCode;

    @UpperCase
    @Column(name = "street", length = 200)
    private String street;

    @UpperCase
    @Column(name = "number", length = 20)
    private String number;

    @UpperCase
    @Column(name = "district", length = 100)
    private String district;

    @UpperCase
    @Column(name = "city", length = 100)
    private String city;

    @UpperCase
    @Column(name = "state", length = 2)
    private String state;

    @UpperCase
    @Column(name = "complement", length = 100)
    private String complement;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrganizationStatus status;

    /**
     * Prefixo do código das Propostas Técnicas emitidas por esta
     * Organization (ex.: {@code "PT"} para Top Power Engenharia,
     * {@code "PL"} para Top Power Materiais). O código final é montado
     * como {@code <prefix>-<sequence 3 dígitos>-<year>} — sequência
     * gerada independentemente por Organization/ano.
     *
     * <p>Obrigatório e único no sistema (constraint {@code UNIQUE} criada
     * pela migration {@code V25}). Imutável no cadastro; pode ser
     * alterado via PATCH (com validação de unicidade).</p>
     */
    @UpperCase
    @Column(name = "proposal_prefix", nullable = false, length = 10)
    private String proposalPrefix;

    @PrePersist
    private void onPrePersist() {
        if (status == null) {
            status = OrganizationStatus.ATIVO;
        }
    }
}