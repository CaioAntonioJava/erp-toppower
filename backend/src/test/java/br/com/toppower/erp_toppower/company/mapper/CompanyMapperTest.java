package br.com.toppower.erp_toppower.company.mapper;

import br.com.toppower.erp_toppower.common.dto.AddressDto;
import br.com.toppower.erp_toppower.common.embeddable.Address;
import br.com.toppower.erp_toppower.common.enums.RegistrationStatus;
import br.com.toppower.erp_toppower.company.dto.CompanyCreateRequest;
import br.com.toppower.erp_toppower.company.dto.CompanyUpdateRequest;
import br.com.toppower.erp_toppower.company.entity.Company;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários de {@link CompanyMapper}.
 *
 * <p>Cobre toEntity, toResponse, applyUpdate e conversão de Address.</p>
 */
class CompanyMapperTest {

    private final AddressDto addressDto = new AddressDto(
            "Rua Exemplo", "100", "Sala 1", "Centro", "São Paulo", "SP", "01310-100");

    @Test
    void toEntity_mapeiaCamposCorretamente() {
        CompanyCreateRequest request = new CompanyCreateRequest(
                "Empresa Exemplo Ltda", "Empresa Exemplo", "11.222.333/0001-81",
                "123456789", true, "987654",
                addressDto, RegistrationStatus.ATIVO);

        Company result = CompanyMapper.toEntity(request);

        assertEquals("Empresa Exemplo Ltda", result.getLegalName());
        assertEquals("Empresa Exemplo", result.getTradeName());
        assertEquals("11.222.333/0001-81", result.getCnpj());
        assertEquals("123456789", result.getStateRegistration());
        assertTrue(result.isStateRegistrationExempt());
        assertEquals("987654", result.getMunicipalRegistration());
        assertEquals(RegistrationStatus.ATIVO, result.getStatus());
        assertNotNull(result.getAddress());
        assertEquals("Rua Exemplo", result.getAddress().getStreet());
    }

    @Test
    void toEntity_stateRegistrationExemptFalse_quandoNaoInformado() {
        CompanyCreateRequest request = new CompanyCreateRequest(
                "Empresa Ltda", null, "11.222.333/0001-81",
                null, null, null,
                addressDto, null);

        Company result = CompanyMapper.toEntity(request);
        assertFalse(result.isStateRegistrationExempt());
    }

    @Test
    void toResponse_mapeiaCamposCorretamente() {
        Company company = new Company();
        company.setId(1L);
        company.setLegalName("Empresa Ltda");
        company.setTradeName("Empresa");
        company.setCode("EMP000001");
        company.setCnpj("11.222.333/0001-81");
        company.setStateRegistration("123");
        company.setStateRegistrationExempt(false);
        company.setMunicipalRegistration("456");
        Address addr = new Address();
        addr.setStreet("Rua A");
        addr.setNumber("10");
        addr.setCity("São Paulo");
        addr.setState("SP");
        company.setAddress(addr);
        company.setStatus(RegistrationStatus.ATIVO);

        var response = CompanyMapper.toResponse(company);

        assertEquals(1L, response.id());
        assertEquals("Empresa Ltda", response.legalName());
        assertEquals("EMP000001", response.code());
        assertEquals("Rua A", response.address().street());
        assertEquals(RegistrationStatus.ATIVO, response.status());
    }

    @Test
    void applyUpdate_camposNaoNulos_atualiza() {
        Company company = new Company();
        company.setLegalName("Original");
        company.setTradeName("Original Trade");
        company.setStateRegistration("IE123");
        company.setStateRegistrationExempt(false);
        company.setMunicipalRegistration("IM123");
        Address addr = new Address();
        addr.setStreet("Rua Original");
        addr.setNumber("1");
        addr.setCity("São Paulo");
        addr.setState("SP");
        company.setAddress(addr);
        company.setStatus(RegistrationStatus.ATIVO);

        AddressDto newAddress = new AddressDto("Rua Nova", "200", null, "Centro", "Rio de Janeiro", "RJ", "20000-000");
        CompanyUpdateRequest update = new CompanyUpdateRequest(
                "Novo Nome", "Novo Trade", "IE456", true, "IM456", newAddress, RegistrationStatus.INATIVO);

        CompanyMapper.applyUpdate(company, update);

        assertEquals("Novo Nome", company.getLegalName());
        assertEquals("Novo Trade", company.getTradeName());
        assertEquals("IE456", company.getStateRegistration());
        assertTrue(company.isStateRegistrationExempt());
        assertEquals("IM456", company.getMunicipalRegistration());
        assertEquals("Rua Nova", company.getAddress().getStreet());
        assertEquals(RegistrationStatus.INATIVO, company.getStatus());
    }

    @Test
    void applyUpdate_camposNulos_naoAltera() {
        Company company = new Company();
        company.setLegalName("Original");
        company.setStatus(RegistrationStatus.ATIVO);

        CompanyUpdateRequest update = new CompanyUpdateRequest(
                null, null, null, null, null, null, null);

        CompanyMapper.applyUpdate(company, update);

        assertEquals("Original", company.getLegalName());
        assertEquals(RegistrationStatus.ATIVO, company.getStatus());
    }

    @Test
    void addressToEntity_mapeiaCorretamente() {
        Address result = CompanyMapper.toEntity(addressDto);
        assertEquals("Rua Exemplo", result.getStreet());
        assertEquals("100", result.getNumber());
        assertEquals("Sala 1", result.getComplement());
        assertEquals("Centro", result.getNeighborhood());
        assertEquals("São Paulo", result.getCity());
        assertEquals("SP", result.getState());
        assertEquals("01310-100", result.getZipCode());
    }

    @Test
    void addressToDto_mapeiaCorretamente() {
        Address address = new Address();
        address.setStreet("Rua Exemplo");
        address.setNumber("100");
        address.setComplement("Sala 1");
        address.setNeighborhood("Centro");
        address.setCity("São Paulo");
        address.setState("SP");
        address.setZipCode("01310-100");

        AddressDto result = CompanyMapper.toDto(address);
        assertEquals("Rua Exemplo", result.street());
        assertEquals("100", result.number());
        assertEquals("Sala 1", result.complement());
        assertEquals("Centro", result.neighborhood());
        assertEquals("São Paulo", result.city());
        assertEquals("SP", result.state());
        assertEquals("01310-100", result.zipCode());
    }
}
