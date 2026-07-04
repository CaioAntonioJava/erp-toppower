package br.com.toppower.erp_toppower.carrier.mapper;

import br.com.toppower.erp_toppower.carrier.dto.CarrierCreateRequest;
import br.com.toppower.erp_toppower.carrier.dto.CarrierResponse;
import br.com.toppower.erp_toppower.carrier.dto.CarrierUpdateRequest;
import br.com.toppower.erp_toppower.carrier.entity.Carrier;

public final class CarrierMapper {

    private CarrierMapper() {
    }

    /**
     * Cria uma nova entidade a partir do request de criação.
     * O {@code status} pode ser {@code null}; o {@code @PrePersist} da entidade
     * cuida de aplicar o default {@code ATIVO}. A normalização para MAIÚSCULAS
     * do {@code name} é feita pelo {@code UpperCaseFieldListener} na gravação.
     */
    public static Carrier toEntity(CarrierCreateRequest request) {
        Carrier carrier = new Carrier();
        carrier.setName(request.name());
        carrier.setFreightValue(request.freightValue());
        carrier.setStatus(request.status());
        return carrier;
    }

    public static CarrierResponse toResponse(Carrier carrier) {
        return new CarrierResponse(
                carrier.getUuid(),
                carrier.getName(),
                carrier.getFreightValue(),
                carrier.getStatus(),
                carrier.getCreatedAt(),
                carrier.getUpdatedAt(),
                carrier.getCreatedBy(),
                carrier.getUpdatedBy()
        );
    }

    /**
     * Aplica uma atualização parcial (PATCH) na entidade carregada.
     * Apenas campos não nulos do request sobrescrevem o estado atual.
     * A normalização para MAIÚSCULAS do {@code name} é feita pelo
     * {@code UpperCaseFieldListener} na gravação.
     */
    public static void applyUpdate(Carrier carrier, CarrierUpdateRequest request) {
        if (request.name() != null) {
            carrier.setName(request.name());
        }
        if (request.freightValue() != null) {
            carrier.setFreightValue(request.freightValue());
        }
        if (request.status() != null) {
            carrier.setStatus(request.status());
        }
    }
}
