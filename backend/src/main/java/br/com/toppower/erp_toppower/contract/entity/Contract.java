package br.com.toppower.erp_toppower.contract.entity;

import br.com.toppower.erp_toppower.common.entity.OrganizationScopedEntity;
import br.com.toppower.erp_toppower.contract.enums.ContractStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entidade que representa um contrato de prestação de serviços emitido
 * pela empresa (Organization) para um cliente pessoa física (Customer)
 * ou jurídica (Company).
 *
 * <p>O identificador comercial do contrato é composto por três campos
 * persistidos — {@link #prefix} (definido pela {@code Organization} que
 * emite, ex.: {@code "CL"} para Top Power Materiais ou {@code "CT"} para
 * Top Power Engenharia), {@link #sequence} (numérico sequencial) e
 * {@link #year} (ano corrente) — e exibido no formato
 * {@code <prefix>-<3 dígitos>-<year>} (ex.: {@code CL-001-2026}) através
 * de {@link #formattedCode()}. A sequência reseta a cada novo ano
 * (volta para {@code 1} quando o ano muda) <b>e é independente por
 * Organization</b>. A trinca {@code (organization_uuid, prefix, sequence,
 * year)} é única no sistema (constraint {@code uk_contract_org_code}).</p>
 *
 * <p>O cliente é referenciado por exatamente <b>um</b> dos campos
 * {@link #customerId} ou {@link #companyId}; o serviço de aplicação
 * valida essa invariante antes de persistir.</p>
 *
 * <p>O {@link #title} é pré-preenchido pelo backend no momento da criação
 * como {@code "CONTRATO DE PRESTAÇÃO DE SERVIÇOS: <código formatado>"}.
 * O {@link #description} armazena o texto livre (HTML ou texto puro) com
 * o conteúdo detalhado do contrato, persistido como {@code TEXT} para
 * suportar formatação rica.</p>
 *
 * <p><b>Organization-scoped</b>: herda de {@link OrganizationScopedEntity}
 * (que estende {@code BaseEntity}) para garantir isolamento multi-tenant
 * via {@code organizationFilter} e permitir que o prefixo do código seja
 * resolvido a partir da Organization ativa.</p>
 */
@Entity
@Table(
        name = "contracts",
        indexes = {
                @Index(name = "idx_contract_status", columnList = "status"),
                @Index(name = "idx_contract_validity_date", columnList = "validity_date"),
                @Index(name = "idx_contract_customer", columnList = "customer_id"),
                @Index(name = "idx_contract_company", columnList = "company_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_contract_org_code",
                        columnNames = {"organization_uuid", "prefix", "sequence", "year"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Contract extends OrganizationScopedEntity {

    /**
     * Prefixo do código comercial (ex.: {@code "CT"} para Top Power
     * Engenharia, {@code "CL"} para Top Power Materiais), copiado da
     * {@code Organization} emissora no momento da criação. Imutável após
     * a criação ({@code updatable = false}).
     */
    @Column(name = "prefix", nullable = false, updatable = false, length = 10)
    private String prefix;

    /**
     * Numeral sequencial do código, reiniciando em {@code 1} a cada novo
     * ano. Exibido com 3 dígitos no código formatado ({@code CT-001-2026}).
     * Imutável após a criação ({@code updatable = false}).
     */
    @Column(name = "sequence", nullable = false, updatable = false)
    private Long sequence;

    /**
     * Ano de emissão do contrato, parte final do código
     * (ex.: {@code 2026} em {@code CT-001-2026}). Imutável após a criação
     * ({@code updatable = false}).
     */
    @Column(name = "year", nullable = false, updatable = false)
    private Integer year;

    /**
     * Referência ao {@code Customer} (pessoa física) contratado.
     * Deve ser preenchido <b>apenas</b> quando o contratado for pessoa
     * física, em conjunto com {@link #companyId} nulo.
     */
    @Column(name = "customer_id")
    private Long customerId;

    /**
     * Referência à {@code Company} (pessoa jurídica) contratada.
     * Deve ser preenchido <b>apenas</b> quando o contratado for pessoa
     * jurídica, em conjunto com {@link #customerId} nulo.
     */
    @Column(name = "company_id")
    private Long companyId;

    /**
     * Título do contrato, pré-preenchido pelo backend no momento da
     * criação como {@code "CONTRATO DE PRESTAÇÃO DE SERVIÇOS: <código>"}.
     * Pode ser editado livremente pelo usuário após a geração inicial.
     */
    @Column(name = "title", nullable = false, length = 300)
    private String title;

    /**
     * Descrição detalhada do contrato em texto livre (HTML ou texto
     * puro). Persistida como {@code TEXT} para suportar formatação rica
     * (parágrafos, negrito, listas, etc.). Pode ser pré-preenchida com o
     * template padrão da {@code Organization}
     * ({@code contract_default_description}) no momento da criação.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Status atual do contrato. Padrão {@link ContractStatus#ATIVO} na
     * criação. O soft delete apenas troca para {@link ContractStatus#INATIVO}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ContractStatus status;

    /**
     * Data de vigência do contrato (data comercial, não timestamp).
     * Representa a data a partir da qual o contrato passa a valer.
     * Sempre definida como {@code LocalDate.now()} no momento da criação.
     */
    @Column(name = "validity_date", nullable = false)
    private LocalDate validityDate;

    /**
     * Preço do contrato, de preenchimento obrigatório pelo usuário.
     * Valor informativo usado para controle interno — <b>não</b> é
     * exibido no template de PDF do contrato (o valor comercial aparece
     * apenas nas cláusulas, que são livremente editáveis).
     */
    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    // ---------------------------------------------------------------------
    // Código formatado
    // ---------------------------------------------------------------------

    /**
     * Código de exibição no formato {@code CT-001-2026}, derivado de
     * {@link #prefix}, {@link #sequence} e {@link #year}.
     */
    public String formattedCode() {
        return String.format("%s-%03d-%d", prefix, sequence, year);
    }

    /**
     * Monta o título padrão de um contrato a partir do código formatado:
     * {@code "CONTRATO DE PRESTAÇÃO DE SERVIÇOS: <prefix>-<seq>-<year>"}.
     * Utilizado pelo service no momento da criação para pré-preencher
     * {@link #title}.
     */
    public static String defaultTitle(String prefix, long sequence, int year) {
        return "CONTRATO DE PRESTAÇÃO DE SERVIÇOS: "
                + String.format("%s-%03d-%d", prefix, sequence, year);
    }

    // ---------------------------------------------------------------------
    // PrePersist
    // ---------------------------------------------------------------------

    /**
     * Inicialização padrão antes de persistir: define o status como
     * {@link ContractStatus#ATIVO} e a data de vigência como
     * {@code LocalDate.now()} quando não tenham sido definidos pelo
     * chamador. Não sobrescreve valores previamente atribuídos.
     */
    @PrePersist
    private void onPrePersist() {
        if (status == null) {
            status = ContractStatus.ATIVO;
        }
        // A data de vigência é sempre a data atual no momento da
        // persistência, independentemente de qualquer valor informado
        // externamente.
        if (validityDate == null) {
            validityDate = LocalDate.now();
        }
    }
}