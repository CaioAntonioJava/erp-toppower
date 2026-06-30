package br.com.toppower.erp_toppower.seller.mapper;

import br.com.toppower.erp_toppower.seller.dto.SellerCreateRequest;
import br.com.toppower.erp_toppower.seller.dto.SellerResponse;
import br.com.toppower.erp_toppower.seller.dto.SellerUpdateRequest;
import br.com.toppower.erp_toppower.seller.entity.Seller;

public final class SellerMapper {

    private SellerMapper() {
    }

    /**
     * Cria uma nova entidade a partir do request de criação.
     */
    public static Seller toEntity(SellerCreateRequest request) {
        Seller seller = new Seller();
        seller.setName(request.name());
        seller.setEmail(request.email());
        seller.setPhone(request.phone());
        seller.setCpf(request.cpf());
        seller.setCommissionRate(request.commissionRate());
        return seller;
    }

    public static SellerResponse toResponse(Seller seller) {
        return new SellerResponse(
                seller.getUuid(),
                seller.getName(),
                seller.getEmail(),
                seller.getPhone(),
                seller.getCpf(),
                seller.getCommissionRate(),
                seller.getCreatedAt(),
                seller.getUpdatedAt(),
                seller.getCreatedBy(),
                seller.getUpdatedBy()
        );
    }

    /**
     * Aplica uma atualização parcial (PATCH) na entidade carregada.
     * Apenas campos não nulos do request sobrescrevem o estado atual.
     */
    public static void applyUpdate(Seller seller, SellerUpdateRequest request) {
        if (request.name() != null) {
            seller.setName(request.name());
        }
        if (request.email() != null) {
            seller.setEmail(request.email());
        }
        if (request.phone() != null) {
            seller.setPhone(request.phone());
        }
        if (request.cpf() != null) {
            seller.setCpf(request.cpf());
        }
        if (request.commissionRate() != null) {
            seller.setCommissionRate(request.commissionRate());
        }
    }
}
