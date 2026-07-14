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
     * O {@code status} pode ser {@code null}; o {@code @PrePersist} da entidade
     * cuida de aplicar o default {@code ATIVO}.
     */
    public static Seller toEntity(SellerCreateRequest request) {
        Seller seller = new Seller();
        seller.setName(request.name());
        seller.setEmail(request.email());
        seller.setPhone(request.phone());
        seller.setCpf(request.cpf());
        seller.setCommissionRate(request.commissionRate());
        seller.setStatus(request.status());
        return seller;
    }

    public static SellerResponse toResponse(Seller seller) {
        return new SellerResponse(
                seller.getId(),
                seller.getName(),
                seller.getEmail(),
                seller.getPhone(),
                seller.getCpf(),
                seller.getCommissionRate(),
                seller.getStatus(),
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
        if (request.status() != null) {
            seller.setStatus(request.status());
        }
    }
}
