package br.com.toppower.erp_toppower.servicetemplate.mapper;

import br.com.toppower.erp_toppower.servicetemplate.dto.ServiceTemplateCreateRequest;
import br.com.toppower.erp_toppower.servicetemplate.dto.ServiceTemplateUpdateRequest;
import br.com.toppower.erp_toppower.servicetemplate.entity.ServiceTemplate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários de {@link ServiceTemplateMapper}.
 *
 * <p>Cobre toEntity, toResponse e applyUpdate.</p>
 */
class ServiceTemplateMapperTest {

    private static final Long CATEGORY_ID = 1L;
    private static final String CATEGORY_NAME = "EXECUÇÃO SPDA";

    @Test
    void toEntity_mapeiaCamposCorretamente() {
        ServiceTemplateCreateRequest request = new ServiceTemplateCreateRequest(
                "Instalação Elétrica", "Serviço de instalação elétrica residencial",
                CATEGORY_ID);

        ServiceTemplate result = ServiceTemplateMapper.toEntity(request);

        assertEquals("Instalação Elétrica", result.getName());
        assertEquals("Serviço de instalação elétrica residencial", result.getDescription());
        assertEquals(CATEGORY_ID, result.getCategoryId());
    }

    @Test
    void toResponse_mapeiaCamposCorretamente() {
        ServiceTemplate entity = new ServiceTemplate();
        entity.setId(1L);
        entity.setName("Manutenção");
        entity.setDescription("Descrição da manutenção");
        entity.setCategoryId(CATEGORY_ID);

        var response = ServiceTemplateMapper.toResponse(entity, CATEGORY_NAME);

        assertEquals(1L, response.id());
        assertEquals("Manutenção", response.name());
        assertEquals("Descrição da manutenção", response.description());
        assertEquals(CATEGORY_ID, response.categoryId());
        assertEquals(CATEGORY_NAME, response.categoryName());
    }

    @Test
    void applyUpdate_camposNaoNulos_atualiza() {
        ServiceTemplate entity = new ServiceTemplate();
        entity.setName("Original");
        entity.setDescription("Desc original");
        entity.setCategoryId(CATEGORY_ID);

        ServiceTemplateUpdateRequest update = new ServiceTemplateUpdateRequest(
                "Novo Nome", "Nova descrição", 2L);

        ServiceTemplateMapper.applyUpdate(entity, update);

        assertEquals("Novo Nome", entity.getName());
        assertEquals("Nova descrição", entity.getDescription());
        assertEquals(2L, entity.getCategoryId());
    }

    @Test
    void applyUpdate_camposNulos_naoAltera() {
        ServiceTemplate entity = new ServiceTemplate();
        entity.setName("Original");
        entity.setCategoryId(CATEGORY_ID);

        ServiceTemplateUpdateRequest update = new ServiceTemplateUpdateRequest(
                null, null, null);

        ServiceTemplateMapper.applyUpdate(entity, update);

        assertEquals("Original", entity.getName());
        assertEquals(CATEGORY_ID, entity.getCategoryId());
    }
}