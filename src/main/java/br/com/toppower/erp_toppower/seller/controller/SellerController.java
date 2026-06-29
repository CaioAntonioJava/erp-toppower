package br.com.toppower.erp_toppower.seller.controller;

import br.com.toppower.erp_toppower.seller.service.SellerService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sellers")
@RequiredArgsConstructor
@Tag(name = "Vendedores", description = "Cadastro de vendedores.")
public class SellerController {

    private final SellerService sellerService;
}
