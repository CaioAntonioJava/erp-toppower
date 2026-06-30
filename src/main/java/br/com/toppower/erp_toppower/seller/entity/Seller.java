package br.com.toppower.erp_toppower.seller.entity;

import br.com.toppower.erp_toppower.person.entity.BasePerson;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Entidade que representa um vendedor do sistema.
 *
 * <p><b>Cadeia de herança (em cascata):</b></p>
 * <pre>
 *   Seller
 *     └─ extends BasePerson        (campos: name, email, phone, cpf)
 *           └─ extends BaseEntity  (campos: uuid, createdAt, updatedAt, createdBy, updatedBy)
 * </pre>
 *
 * <p>Embora {@code Seller} declare apenas {@code extends BasePerson},
 * a herança em cadeia garante que ele também herda todos os campos de
 * {@link BaseEntity}: identificador UUID e auditoria completa
 * (createdAt, updatedAt, createdBy, updatedBy).</p>
 *
 * <p>O atributo {@code commissionRate} armazena o percentual de
 * comissão do vendedor, em formato decimal
 * (ex: 5.50 representa 5,50% de comissão sobre a venda).</p>
 */
@Entity
@Table(name = "sellers")
@Getter
@Setter
@NoArgsConstructor
public class Seller extends BasePerson {

    /**
     * Percentual de comissão do vendedor.
     * <p>Exemplos: 5.00 = 5%, 12.50 = 12,5%, 100.00 = 100%.</p>
     * <p>Precisão: até 999.99% (precision=5, scale=2).</p>
     * <p>Campo opcional: aceita {@code null} (vendedor ainda sem comissão
     * configurada) ou {@code 0.00} (vendedor sem comissão).</p>
     */
    @Column(name = "commission_rate", precision = 5, scale = 2)
    private BigDecimal commissionRate;
}
