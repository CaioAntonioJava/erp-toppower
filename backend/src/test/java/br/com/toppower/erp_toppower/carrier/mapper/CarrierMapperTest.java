package br.com.toppower.erp_toppower.carrier.mapper;

import br.com.toppower.erp_toppower.carrier.dto.CarrierCreateRequest;
import br.com.toppower.erp_toppower.carrier.dto.CarrierUpdateRequest;
import br.com.toppower.erp_toppower.carrier.entity.Carrier;
import br.com.toppower.erp_toppower.carrier.enums.CarrierStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários de {@link CarrierMapper}.
 *
 * <p>Cobre toEntity, toResponse e applyUpdate.</p>
 */
class CarrierMapperTest {

    @Test
    void toEntity_mapeiaCamposCorretamente() {
        CarrierCreateRequest request = new CarrierCreateRequest("Transportadora XYZ", CarrierStatus.ATIVO);
        Carrier result = CarrierMapper.toEntity(request);

        assertEquals("Transportadora XYZ", result.getName());
        assertEquals(CarrierStatus.ATIVO, result.getStatus());
    }

    @Test
    void toEntity_statusNulo_naoAplicaDefault() {
        CarrierCreateRequest request = new CarrierCreateRequest("Transportadora", null);
        Carrier result = CarrierMapper.toEntity(request);
        assertNull(result.getStatus());
    }

    @Test
    void toResponse_mapeiaCamposCorretamente() {
        Carrier carrier = new Carrier();
        carrier.setId(1L);
        carrier.setName("Transportadora ABC");
        carrier.setStatus(CarrierStatus.ATIVO);

        var response = CarrierMapper.toResponse(carrier);

        assertEquals(1L, response.id());
        assertEquals("Transportadora ABC", response.name());
        assertEquals(CarrierStatus.ATIVO, response.status());
    }

    @Test
    void applyUpdate_camposNaoNulos_atualiza() {
        Carrier carrier = new Carrier();
        carrier.setName("Original");
        carrier.setStatus(CarrierStatus.ATIVO);

        CarrierUpdateRequest update = new CarrierUpdateRequest("Nova Transportadora", CarrierStatus.INATIVO);
        CarrierMapper.applyUpdate(carrier, update);

        assertEquals("Nova Transportadora", carrier.getName());
        assertEquals(CarrierStatus.INATIVO, carrier.getStatus());
    }

    @Test
    void applyUpdate_camposNulos_naoAltera() {
        Carrier carrier = new Carrier();
        carrier.setName("Original");
        carrier.setStatus(CarrierStatus.ATIVO);

        CarrierUpdateRequest update = new CarrierUpdateRequest(null, null);
        CarrierMapper.applyUpdate(carrier, update);

        assertEquals("Original", carrier.getName());
        assertEquals(CarrierStatus.ATIVO, carrier.getStatus());
    }
}
