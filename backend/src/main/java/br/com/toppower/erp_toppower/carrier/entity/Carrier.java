package br.com.toppower.erp_toppower.carrier.entity;

import br.com.toppower.erp_toppower.carrier.enums.CarrierStatus;
import br.com.toppower.erp_toppower.common.annotation.UpperCase;
import br.com.toppower.erp_toppower.common.entity.OrganizationScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "carriers")
@Getter
@Setter
@NoArgsConstructor
public class Carrier extends OrganizationScopedEntity {

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