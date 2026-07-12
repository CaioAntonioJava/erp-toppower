package br.com.toppower.erp_toppower.contract.entity;

import br.com.toppower.erp_toppower.common.embeddable.Address;
import br.com.toppower.erp_toppower.common.entity.OrganizationScopedEntity;
import br.com.toppower.erp_toppower.contract.enums.ContractStatus;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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
 * Entidade que representa um contrato emitido pela empresa para um cliente
 * pessoa física ({@code Customer}) ou jurídica ({@code Company}).
 *
 * <p>O identificador comercial do contrato é composto por três campos
 * persistidos — {@link #prefix} (definido pela {@code Organization} que
 * emite, ex.: {@code "CT"} para Top Power Engenharia ou {@code "CL"} para
 * Top Power Materiais), {@link #sequence} (numérico sequencial) e
 * {@link #year} (ano corrente) — e exibido no formato
 * {@code <prefix>-<3 dígitos>-<year>} (ex.: {@code CT-001-2026}) através
 * de {@link #formattedCode()}. A sequência reseta a {@code 1} a cada novo
 * ano <b>e é independente por Organization</b>. A trinca
 * {@code (organization_uuid, prefix, sequence, year)} é única no sistema
 * (constraint {@code uk_contract_org_code}).</p>
 *
 * <p>O cliente é referenciado pelo campo {@link #customerUuid}; esta
 * primeira versão do agregado atende apenas clientes pessoa física — a
 * referência a pessoas jurídicas ({@code Company}) pode ser adicionada em
 * uma evolução futura, mantendo o mesmo padrão de UUID.</p>
 *
 * <p>O endereço é opcional e, quando preenchido, é sugerido a partir do
 * endereço do cliente selecionado no formulário — todos os campos do
 * {@link Address} ficam nullable aqui, em contraste com
 * {@code Customer}/{@code Company}, onde são obrigatórios.</p>
 *
 * <p>Esta entidade estende {@link OrganizationScopedEntity} (que por sua
 * vez estende {@code BaseEntity}), o que garante:</p>
 * <ul>
 *   <li>{@code uuid} (PK) + auditoria (createdAt/updatedAt/createdBy/
 *       updatedBy);</li>
 *   <li>coluna {@code organization_uuid} com filtro Hibernate automático,
 *       isolando contratos por Organization;</li>
 *   <li>preenchimento automático do {@code organization_uuid} no persist
 *       via {@code OrganizationEntityListener} + {@code OrganizationContext}.</li>
 * </ul>
 */
@Entity
@Table(
        name = "contracts",
        indexes = {
                @Index(name = "idx_contract_status", columnList = "status"),
                @Index(name = "idx_contract_start_date", columnList = "start_date"),
                @Index(name = "idx_contract_customer", columnList = "customer_uuid"),
                @Index(name = "idx_contract_company", columnList = "company_uuid")
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

    /** Prefixo de fallback do código. Em produção vem de {@code Organization.contractPrefix}. */
    public static final String DEFAULT_PREFIX = "CT";

    /**
     * Prefixo do código comercial (ex.: {@code "CT"} ou {@code "CL"}),
     * copiado da {@code Organization} emissora no momento da criação.
     * Imutável após a criação ({@code updatable = false}).
     */
    @Column(name = "prefix", nullable = false, updatable = false, length = 10)
    private String prefix;

    /**
     * Numeral sequencial do código, reiniciando em {@code 1} a cada novo
     * ano. Exibido com 3 dígitos no código formatado
     * ({@code CT-001-2026}). Imutável após a criação
     * ({@code updatable = false}).
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
     * Referência ao {@code Customer} (pessoa física) contratante.
     * Opcional — exatamente <b>um</b> entre {@link #customerUuid} e
     * {@link #companyUuid} deve estar preenchido (validação no service).
     */
    @Column(name = "customer_uuid")
    private java.util.UUID customerUuid;

    /**
     * Referência à {@code Company} (pessoa jurídica) contratante.
     * Opcional — exatamente <b>um</b> entre {@link #customerUuid} e
     * este campo deve estar preenchido (validação no service).
     */
    @Column(name = "company_uuid")
    private java.util.UUID companyUuid;

    /**
     * Endereço vinculado ao contrato (ex.: local de execução). <b>Opcional</b>
     * — todos os campos do {@link Address} são nullable aqui. Quando
     * preenchido, é tipicamente sugerido a partir do endereço do cliente
     * selecionado, mas pode ser livremente editado.
     */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "street",
                    column = @Column(name = "address_street", length = 200)),
            @AttributeOverride(name = "number",
                    column = @Column(name = "address_number", length = 20)),
            @AttributeOverride(name = "complement",
                    column = @Column(name = "address_complement", length = 100)),
            @AttributeOverride(name = "neighborhood",
                    column = @Column(name = "address_neighborhood", length = 100)),
            @AttributeOverride(name = "city",
                    column = @Column(name = "address_city", length = 100)),
            @AttributeOverride(name = "state",
                    column = @Column(name = "address_state", length = 2)),
            @AttributeOverride(name = "zipCode",
                    column = @Column(name = "address_zip_code", length = 9))
    })
    private Address address;

    /**
     * Descrição detalhada do contrato. Bloco de texto longo para digitação
     * (em torno de 1000 caracteres), descrevendo o objeto/escopo do
     * contrato. Persistido como {@code TEXT} para tolerar conteúdo
     * extenso.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Descrição dos serviços prestados no contrato. <b>Opcional</b>,
     * bloco de texto livre (sem linhas/itens estruturados nesta versão —
     * segue o padrão pedido pelo usuário, idêntico ao campo de serviços
     * da Proposta Técnica porém sem preço).
     */
    @Column(name = "services_description", columnDefinition = "TEXT")
    private String servicesDescription;

    /**
     * Descrição dos produtos incluídos no contrato. <b>Opcional</b>,
     * bloco de texto livre. Segue o padrão pedido pelo usuário — campo
     * similar ao de produtos da Proposta Técnica, porém sem linhas
     * estruturadas (quantidade, desconto, preço por item) nesta versão.
     */
    @Column(name = "products_description", columnDefinition = "TEXT")
    private String productsDescription;

    /**
     * Prazo de entrega do contrato. <b>Opcional</b>, texto livre
     * (ex.: "30 dias úteis", "15 dias após a assinatura", "entrega
     * imediata"). Persistido como {@code VARCHAR(500)} — não tem
     * semântica de data; é apenas uma descrição que aparece no
     * formulário abaixo de "Cláusula 2" e no PDF.
     */
    @Column(name = "delivery_deadline", length = 500)
    private String deliveryDeadline;

    /**
     * Bloco de texto adicional ao final do contrato. <b>Opcional</b>,
     * rich text (HTML) — segue o mesmo padrão de
     * {@link #description}, {@link #servicesDescription} e
     * {@link #productsDescription}. Aparece no formulário abaixo da
     * seção "Cláusula 3" e antes de "Valor total", e é renderizado no
     * PDF na mesma posição. Persistido como {@code TEXT} sem limite
     * rígido.
     */
    @Column(name = "additional_description", columnDefinition = "TEXT")
    private String additionalDescription;

    /**
     * Status atual do contrato no seu ciclo de vida. Padrão
     * {@link ContractStatus#ABERTA} na criação.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ContractStatus status;

    /**
     * Data de início da vigência do contrato (data comercial, não
     * timestamp). Obrigatória; recebe {@code LocalDate.now()} no
     * {@link #onPrePersist()} caso não tenha sido informada.
     */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /**
     * Valor total do contrato (preenchimento manual pelo usuário).
     * Opcional — sem cálculo automático.
     */
    @Column(name = "total_value", precision = 12, scale = 2)
    private BigDecimal totalValue;

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

    // ---------------------------------------------------------------------
    // PrePersist
    // ---------------------------------------------------------------------

    /**
     * Inicialização padrão antes de persistir: define o prefixo como
     * {@link #DEFAULT_PREFIX} (fallback), o status como
     * {@link ContractStatus#ABERTA} e a data de início como
     * {@code LocalDate.now()} quando não tenham sido definidos pelo
     * chamador. Não sobrescreve valores previamente atribuídos.
     *
     * <p>Em produção, o service injeta o prefixo a partir da
     * {@code Organization} ativa ({@code OrganizationContext.contractPrefix});
     * este fallback só é acionado em cenários legados/bootstrap.</p>
     */
    @PrePersist
    private void onPrePersist() {
        if (prefix == null) {
            prefix = DEFAULT_PREFIX;
        }
        if (status == null) {
            status = ContractStatus.ABERTA;
        }
        if (startDate == null) {
            startDate = LocalDate.now();
        }
    }
}