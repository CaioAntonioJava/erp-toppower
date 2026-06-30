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
 * <p>Herdar {@link BasePerson} garante os campos compartilhados
 * (name, email, phone, cpf). A herança em cadeia inclui também
 * {@code BaseEntity}, que fornece identificador UUID e auditoria
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
     * <p>Campo obrigatório.</p>
     */
    @Column(name = "commission_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal commissionRate;
}
