package br.com.toppower.erp_toppower.product.entity;

import br.com.toppower.erp_toppower.common.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "products")
public class Product extends BaseEntity {
}
