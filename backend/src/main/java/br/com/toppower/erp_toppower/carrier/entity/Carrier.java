package br.com.toppower.erp_toppower.carrier.entity;

import br.com.toppower.erp_toppower.carrier.enums.CarrierStatus;
import br.com.toppower.erp_toppower.common.annotation.UpperCase;
import br.com.toppower.erp_toppower.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Transportadora responsável pelo frete de propostas comerciais, propostas
 * técnicas e pedidos de venda.
 *
 * <p>Entidade <strong>global</strong>: não herda de {@code
 * OrganizationScopedEntity} e, portanto, não é isolada por organização.
 * As transportadoras cadastradas são compartilhadas entre todas as empresas,
 * alinhando-se a domínios de referência como {@code Profile} e {@code Cep}.
 * Assim, qualquer usuário autenticado (ADMIN/MANAGER/SELLER) consegue listar
 * as transportadoras no dropdown dos formulários de venda, sem depender da
 * empresa ativa.</p>
 */
@Entity
@Table(name = "carriers")
@Getter
@Setter
@NoArgsConstructor
public class Carrier extends BaseEntity {

    @UpperCase
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CarrierStatus status;

    @PrePersist
    private void onPrePersist() {
        if (status == null) {
            status = CarrierStatus.ATIVO;
        }
    }
}