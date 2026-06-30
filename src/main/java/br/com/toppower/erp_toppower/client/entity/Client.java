package br.com.toppower.erp_toppower.client.entity;

import br.com.toppower.erp_toppower.client.enums.PersonType;
import br.com.toppower.erp_toppower.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidade que representa um cliente do sistema (pessoa física ou jurídica).
 *
 * <p>Herdar {@link BaseEntity} fornece o identificador UUID e a auditoria
 * completa (createdAt, updatedAt, createdBy, updatedBy).</p>
 *
 * <p>Um cliente pode ser tanto uma pessoa física (CPF) quanto uma
 * pessoa jurídica (CNPJ), definido pelo enum {@link PersonType}.</p>
 */
@Entity
@Table(name = "clients")
@Getter
@Setter
@NoArgsConstructor
public class Client extends BaseEntity {

    /**
     * Razão social — nome oficial/registrado do cliente.
     * Obrigatório.
     */
    @Column(name = "legal_name", nullable = false, length = 200)
    private String legalName;

    /**
     * Nome fantasia — nome comercial do cliente.
     * Opcional (ex: pessoa física pode não ter).
     */
    @Column(name = "trade_name", length = 200)
    private String tradeName;

    /**
     * Código interno único do cliente (ex: "CLI-001").
     * Obrigatório e único.
     */
    @Column(name = "code", unique = true, nullable = false, length = 50)
    private String code;

    /**
     * Tipo de pessoa: física (CPF) ou jurídica (CNPJ).
     * Obrigatório.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "person_type", nullable = false, length = 20)
    private PersonType personType;

    /**
     * Documento fiscal: CPF (11 dígitos) ou CNPJ (14 dígitos),
     * conforme o {@code personType}. Armazenado como String para preservar
     * zeros à esquerda e permitir formatação.
     * Obrigatório e único.
     */
    @Column(name = "tax_id", nullable = false, length = 20, unique = true)
    private String taxId;

    /**
     * Inscrição Estadual (IE) — registro da empresa na Secretaria da Fazenda.
     * Opcional (pode ser nulo quando {@code stateRegistrationExempt = true}).
     */
    @Column(name = "state_registration", length = 30)
    private String stateRegistration;

    /**
     * Inscrição Municipal (IM) — registro da empresa na Prefeitura.
     * Opcional.
     */
    @Column(name = "municipal_registration", length = 30)
    private String municipalRegistration;
}
