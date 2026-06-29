package br.com.toppower.erp_toppower.customer.entity;

import br.com.toppower.erp_toppower.common.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "customers")
public class Customer extends BaseEntity {
}
