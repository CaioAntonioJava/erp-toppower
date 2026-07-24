package br.com.toppower.erp_toppower.servicetemplate.mapper;

import br.com.toppower.erp_toppower.servicetemplate.dto.ServiceTemplateCreateRequest;
import br.com.toppower.erp_toppower.servicetemplate.dto.ServiceTemplateUpdateRequest;
import br.com.toppower.erp_toppower.servicetemplate.entity.ServiceTemplate;
import br.com.toppower.erp_toppower.servicetemplate.enums.ServiceCategory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários de {@link ServiceTemplateMapper}.
 *
 * <p>Cobre toEntity, toResponse e applyUpdate.</p>
 */
class ServiceTemplateMapperTest {

    @Test
    void toEntity_mapeiaCamposCorretamente() {
        ServiceTemplateCreateRequest request = new ServiceTemplateCreateRequest(
                "Instalação Elétrica", "Serviço de instalação elétrica residencial",
                ServiceCategory.EXECUÇÃO_SPDA);

        ServiceTemplate result = ServiceTemplateMapper.toEntity(request);

        assertEquals("Instalação Elétrica", result.getName());
        assertEquals("Serviço de instalação elétrica residencial", result.getDescription());
        assertEquals(ServiceCategory.EXECUÇÃO_SPDA, result.getCategory());
    }

    @Test
    void toResponse_mapeiaCamposCorretamente() {
        ServiceTemplate entity = new ServiceTemplate();
        entity.setId(1L);
        entity.setName("Manutenção");
        entity.setDescription("Descrição da manutenção");
        entity.setCategory(ServiceCategory.EXECUÇÃO_SPDA);

        var response = ServiceTemplateMapper.toResponse(entity);

        assertEquals(1L, response.id());
        assertEquals("Manutenção", response.name());
        assertEquals("Descrição da manutenção", response.description());
        assertEquals(ServiceCategory.EXECUÇÃO_SPDA, response.category());
    }

    @Test
    void applyUpdate_camposNaoNulos_atualiza() {
        ServiceTemplate entity = new ServiceTemplate();
        entity.setName("Original");
        entity.setDescription("Desc original");
        entity.setCategory(ServiceCategory.EXECUÇÃO_SPDA);

        ServiceTemplateUpdateRequest update = new ServiceTemplateUpdateRequest(
                "Novo Nome", "Nova descrição", ServiceCategory.EXECUÇÃO_SPDA);

        ServiceTemplateMapper.applyUpdate(entity, update);

        assertEquals("Novo Nome", entity.getName());
        assertEquals("Nova descrição", entity.getDescription());
        assertEquals(ServiceCategory.EXECUÇÃO_SPDA, entity.getCategory());
    }

    @Test
    void applyUpdate_camposNulos_naoAltera() {
        ServiceTemplate entity = new ServiceTemplate();
        entity.setName("Original");
        entity.setCategory(ServiceCategory.EXECUÇÃO_SPDA);

        ServiceTemplateUpdateRequest update = new ServiceTemplateUpdateRequest(
                null, null, null);

        ServiceTemplateMapper.applyUpdate(entity, update);

        assertEquals("Original", entity.getName());
        assertEquals(ServiceCategory.EXECUÇÃO_SPDA, entity.getCategory());
    }
}
