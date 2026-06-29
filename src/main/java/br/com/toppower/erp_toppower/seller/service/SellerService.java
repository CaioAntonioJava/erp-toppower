package br.com.toppower.erp_toppower.seller.service;

import br.com.toppower.erp_toppower.seller.repository.SellerRepository;
import org.springframework.stereotype.Service;

@Service
public class SellerService {

    private final SellerRepository sellerRepository;

    public SellerService(SellerRepository sellerRepository) {
        this.sellerRepository = sellerRepository;
    }
}
