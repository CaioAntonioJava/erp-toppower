package br.com.toppower.erp_toppower.supplier.controller;

import br.com.toppower.erp_toppower.supplier.service.SupplierService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
@Tag(name = "Fornecedores", description = "Cadastro de fornecedores.")
public class SupplierController {

    private final SupplierService supplierService;
}
