package br.com.toppower.erp_toppower.carrier.mapper;

import br.com.toppower.erp_toppower.carrier.dto.CarrierCreateRequest;
import br.com.toppower.erp_toppower.carrier.dto.CarrierResponse;
import br.com.toppower.erp_toppower.carrier.dto.CarrierUpdateRequest;
import br.com.toppower.erp_toppower.carrier.entity.Carrier;

public final class CarrierMapper {

    private CarrierMapper() {
    }

    /**
     * O {@code status} pode ser {@code null}; o {@code @PrePersist} da entidade
     * cuida de aplicar o default {@code ATIVO}.
     */
    public static Carrier toEntity(CarrierCreateRequest request) {
        Carrier carrier = new Carrier();
        carrier.setName(request.name());
        carrier.setServiceName(request.serviceName());
        carrier.setStatus(request.status());
        return carrier;
    }

    public static CarrierResponse toResponse(Carrier carrier) {
        return new CarrierResponse(
                carrier.getUuid(),
                carrier.getName(),
                carrier.getServiceName(),
                carrier.getStatus(),
                carrier.getCreatedAt(),
                carrier.getUpdatedAt(),
                carrier.getCreatedBy(),
                carrier.getUpdatedBy()
        );
    }

    /**
     * Aplica atualização parcial (PATCH).
     */
    public static void applyUpdate(Carrier carrier, CarrierUpdateRequest request) {
        if (request.name() != null) {
            carrier.setName(request.name());
        }
        if (request.serviceName() != null) {
            carrier.setServiceName(request.serviceName());
        }
        if (request.status() != null) {
            carrier.setStatus(request.status());
        }
    }
}