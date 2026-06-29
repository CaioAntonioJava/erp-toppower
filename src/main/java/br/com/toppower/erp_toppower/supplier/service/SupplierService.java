package br.com.toppower.erp_toppower.supplier.service;

import br.com.toppower.erp_toppower.supplier.repository.SupplierRepository;
import org.springframework.stereotype.Service;

@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;

    public SupplierService(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }
}
