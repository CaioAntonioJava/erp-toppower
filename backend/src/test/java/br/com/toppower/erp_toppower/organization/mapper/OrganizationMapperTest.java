package br.com.toppower.erp_toppower.organization.mapper;

import br.com.toppower.erp_toppower.organization.dto.OrganizationCreateRequest;
import br.com.toppower.erp_toppower.organization.dto.OrganizationUpdateRequest;
import br.com.toppower.erp_toppower.organization.entity.Organization;
import br.com.toppower.erp_toppower.organization.enums.OrganizationStatus;
import br.com.toppower.erp_toppower.user.enums.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários de {@link OrganizationMapper}.
 *
 * <p>Cobre toEntity, toResponse, toSummary e applyUpdate.</p>
 */
class OrganizationMapperTest {

    @Test
    void toEntity_mapeiaCamposCorretamente() {
        OrganizationCreateRequest request = new OrganizationCreateRequest(
                "Top Power Engenharia Ltda", "Top Power Engenharia",
                "11.222.333/0001-81", "123456789", "987654",
                "11999999999", "contato@toppower.com.br",
                "01310-100", "Rua Exemplo", "100", "Centro", "São Paulo", "SP",
                "Sala 1", "/logos/org.png", OrganizationStatus.ATIVO,
                "PT", "CT", "Descrição padrão do contrato");

        Organization result = OrganizationMapper.toEntity(request);

        assertEquals("Top Power Engenharia Ltda", result.getCorporateName());
        assertEquals("Top Power Engenharia", result.getTradeName());
        assertEquals("11.222.333/0001-81", result.getCnpj());
        assertEquals("123456789", result.getStateRegistration());
        assertEquals("987654", result.getMunicipalRegistration());
        assertEquals("11999999999", result.getPhone());
        assertEquals("contato@toppower.com.br", result.getEmail());
        assertEquals("01310-100", result.getZipCode());
        assertEquals("Rua Exemplo", result.getStreet());
        assertEquals("100", result.getNumber());
        assertEquals("Centro", result.getDistrict());
        assertEquals("São Paulo", result.getCity());
        assertEquals("SP", result.getState());
        assertEquals("Sala 1", result.getComplement());
        assertEquals("/logos/org.png", result.getLogoUrl());
        assertEquals(OrganizationStatus.ATIVO, result.getStatus());
        assertEquals("PT", result.getProposalPrefix());
        assertEquals("CT", result.getContractPrefix());
        assertEquals("Descrição padrão do contrato", result.getContractDefaultDescription());
    }

    @Test
    void toResponse_mapeiaCamposCorretamente() {
        Organization org = new Organization();
        org.setId(1L);
        org.setCorporateName("Empresa Ltda");
        org.setTradeName("Empresa");
        org.setCnpj("11.222.333/0001-81");
        org.setStatus(OrganizationStatus.ATIVO);
        org.setProposalPrefix("PT");
        org.setContractPrefix("CT");

        var response = OrganizationMapper.toResponse(org);

        assertEquals(1L, response.id());
        assertEquals("Empresa Ltda", response.corporateName());
        assertEquals("PT", response.proposalPrefix());
        assertEquals("CT", response.contractPrefix());
        assertEquals(OrganizationStatus.ATIVO, response.status());
    }

    @Test
    void toSummary_semRole_retornaNullRole() {
        Organization org = new Organization();
        org.setId(1L);
        org.setCorporateName("Empresa Ltda");
        org.setTradeName("Empresa");
        org.setCnpj("11.222.333/0001-81");
        org.setStatus(OrganizationStatus.ATIVO);
        org.setProposalPrefix("PT");
        org.setContractPrefix("CT");

        var summary = OrganizationMapper.toSummary(org);

        assertEquals(1L, summary.id());
        assertEquals("Empresa Ltda", summary.corporateName());
        assertNull(summary.role());
        assertFalse(summary.isDefault());
    }

    @Test
    void toSummary_comRole_mapeiaCorretamente() {
        Organization org = new Organization();
        org.setId(1L);
        org.setCorporateName("Empresa Ltda");
        org.setTradeName("Empresa");
        org.setCnpj("11.222.333/0001-81");
        org.setStatus(OrganizationStatus.ATIVO);
        org.setProposalPrefix("PT");
        org.setContractPrefix("CT");

        var summary = OrganizationMapper.toSummary(org, Role.ROLE_MANAGER, true);

        assertEquals(Role.ROLE_MANAGER, summary.role());
        assertTrue(summary.isDefault());
    }

    @Test
    void applyUpdate_camposNaoNulos_atualiza() {
        Organization org = new Organization();
        org.setCorporateName("Original");
        org.setTradeName("Original Trade");
        org.setStatus(OrganizationStatus.ATIVO);
        org.setProposalPrefix("PT");
        org.setContractPrefix("CT");

        OrganizationUpdateRequest update = new OrganizationUpdateRequest(
                "Novo Nome", "Novo Trade", "IE456", "IM456",
                "2188888888", "novo@email.com", "20000-000",
                "Rua Nova", "500", "Centro", "Rio de Janeiro", "RJ",
                "Sala 5", "/logos/novo.png", OrganizationStatus.INATIVO,
                "PP", "CC", "Nova descrição");

        OrganizationMapper.applyUpdate(org, update);

        assertEquals("Novo Nome", org.getCorporateName());
        assertEquals("Novo Trade", org.getTradeName());
        assertEquals("IE456", org.getStateRegistration());
        assertEquals("IM456", org.getMunicipalRegistration());
        assertEquals("2188888888", org.getPhone());
        assertEquals("novo@email.com", org.getEmail());
        assertEquals("20000-000", org.getZipCode());
        assertEquals("Rua Nova", org.getStreet());
        assertEquals("500", org.getNumber());
        assertEquals("Centro", org.getDistrict());
        assertEquals("Rio de Janeiro", org.getCity());
        assertEquals("RJ", org.getState());
        assertEquals("Sala 5", org.getComplement());
        assertEquals("/logos/novo.png", org.getLogoUrl());
        assertEquals(OrganizationStatus.INATIVO, org.getStatus());
        assertEquals("PP", org.getProposalPrefix());
        assertEquals("CC", org.getContractPrefix());
        assertEquals("Nova descrição", org.getContractDefaultDescription());
    }

    @Test
    void applyUpdate_camposNulos_naoAltera() {
        Organization org = new Organization();
        org.setCorporateName("Original");
        org.setStatus(OrganizationStatus.ATIVO);
        org.setProposalPrefix("PT");
        org.setContractPrefix("CT");

        OrganizationUpdateRequest update = new OrganizationUpdateRequest(
                null, null, null, null, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null, null);

        OrganizationMapper.applyUpdate(org, update);

        assertEquals("Original", org.getCorporateName());
        assertEquals(OrganizationStatus.ATIVO, org.getStatus());
        assertEquals("PT", org.getProposalPrefix());
        assertEquals("CT", org.getContractPrefix());
    }
}
