package br.com.toppower.erp_toppower.customer.controller;

import br.com.toppower.erp_toppower.customer.service.CustomerService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "Cadastro de clientes.")
public class CustomerController {

    private final CustomerService customerService;
}
