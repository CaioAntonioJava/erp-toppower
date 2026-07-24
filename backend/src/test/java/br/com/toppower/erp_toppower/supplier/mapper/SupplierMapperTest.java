package br.com.toppower.erp_toppower.supplier.mapper;

import br.com.toppower.erp_toppower.common.embeddable.Address;
import br.com.toppower.erp_toppower.supplier.dto.AddressDto;
import br.com.toppower.erp_toppower.supplier.dto.SupplierCreateRequest;
import br.com.toppower.erp_toppower.supplier.dto.SupplierUpdateRequest;
import br.com.toppower.erp_toppower.supplier.entity.Supplier;
import br.com.toppower.erp_toppower.supplier.enums.SupplierStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários de {@link SupplierMapper}.
 *
 * <p>Cobre toEntity, toResponse, applyUpdate e conversão de Address.</p>
 */
class SupplierMapperTest {

    private final AddressDto addressDto = new AddressDto(
            "Rua do Fornecedor", "300", "Galpão 2", "Industrial", "São Paulo", "SP", "01310-300");

    @Test
    void toEntity_mapeiaCamposCorretamente() {
        SupplierCreateRequest request = new SupplierCreateRequest(
                "Fornecedor Ltda", "Fornecedor", "11.222.333/0001-81",
                "123456789", "987654", "contato@fornecedor.com",
                "11999999999", "João Contato", addressDto, SupplierStatus.ATIVO);

        Supplier result = SupplierMapper.toEntity(request);

        assertEquals("Fornecedor Ltda", result.getLegalName());
        assertEquals("Fornecedor", result.getTradeName());
        assertEquals("11.222.333/0001-81", result.getTaxId());
        assertEquals("123456789", result.getStateRegistration());
        assertEquals("987654", result.getMunicipalRegistration());
        assertEquals("contato@fornecedor.com", result.getEmail());
        assertEquals("11999999999", result.getPhone());
        assertEquals("João Contato", result.getContactName());
        assertEquals(SupplierStatus.ATIVO, result.getStatus());
        assertNotNull(result.getAddress());
        assertEquals("Rua do Fornecedor", result.getAddress().getStreet());
    }

    @Test
    void toEntity_statusNulo_naoAplicaDefault() {
        SupplierCreateRequest request = new SupplierCreateRequest(
                "Fornecedor", null, "11.222.333/0001-81",
                null, null, null, null, null, addressDto, null);

        Supplier result = SupplierMapper.toEntity(request);
        assertNull(result.getStatus());
    }

    @Test
    void toResponse_mapeiaCamposCorretamente() {
        Supplier supplier = new Supplier();
        supplier.setId(1L);
        supplier.setLegalName("Fornecedor ABC Ltda");
        supplier.setTradeName("Fornecedor ABC");
        supplier.setTaxId("11.222.333/0001-81");
        supplier.setStateRegistration("IE123");
        supplier.setMunicipalRegistration("IM456");
        supplier.setEmail("abc@fornecedor.com");
        supplier.setPhone("11988888888");
        supplier.setContactName("Maria Contato");
        Address addr = new Address();
        addr.setStreet("Rua ABC");
        addr.setNumber("100");
        addr.setCity("São Paulo");
        addr.setState("SP");
        supplier.setAddress(addr);
        supplier.setStatus(SupplierStatus.ATIVO);

        var response = SupplierMapper.toResponse(supplier);

        assertEquals(1L, response.id());
        assertEquals("Fornecedor ABC Ltda", response.legalName());
        assertEquals("Fornecedor ABC", response.tradeName());
        assertEquals("11.222.333/0001-81", response.taxId());
        assertEquals("Rua ABC", response.address().street());
        assertEquals(SupplierStatus.ATIVO, response.status());
    }

    @Test
    void applyUpdate_camposNaoNulos_atualiza() {
        Supplier supplier = new Supplier();
        supplier.setLegalName("Original");
        supplier.setTradeName("Original Trade");
        supplier.setStateRegistration("IE123");
        supplier.setMunicipalRegistration("IM123");
        supplier.setEmail("original@email.com");
        supplier.setPhone("11111111111");
        supplier.setContactName("Contato Original");
        Address addr = new Address();
        addr.setStreet("Rua Original");
        addr.setNumber("1");
        addr.setCity("São Paulo");
        addr.setState("SP");
        supplier.setAddress(addr);
        supplier.setStatus(SupplierStatus.ATIVO);

        AddressDto newAddress = new AddressDto("Rua Nova", "500", null, "Centro", "Rio de Janeiro", "RJ", "20000-000");
        SupplierUpdateRequest update = new SupplierUpdateRequest(
                "Novo Nome", "Novo Trade", "IE456", "IM456",
                "novo@email.com", "22222222222", "Novo Contato",
                newAddress, SupplierStatus.INATIVO);

        SupplierMapper.applyUpdate(supplier, update);

        assertEquals("Novo Nome", supplier.getLegalName());
        assertEquals("Novo Trade", supplier.getTradeName());
        assertEquals("IE456", supplier.getStateRegistration());
        assertEquals("IM456", supplier.getMunicipalRegistration());
        assertEquals("novo@email.com", supplier.getEmail());
        assertEquals("22222222222", supplier.getPhone());
        assertEquals("Novo Contato", supplier.getContactName());
        assertEquals("Rua Nova", supplier.getAddress().getStreet());
        assertEquals(SupplierStatus.INATIVO, supplier.getStatus());
    }

    @Test
    void applyUpdate_camposNulos_naoAltera() {
        Supplier supplier = new Supplier();
        supplier.setLegalName("Original");
        supplier.setStatus(SupplierStatus.ATIVO);

        SupplierUpdateRequest update = new SupplierUpdateRequest(
                null, null, null, null, null, null, null, null, null);

        SupplierMapper.applyUpdate(supplier, update);

        assertEquals("Original", supplier.getLegalName());
        assertEquals(SupplierStatus.ATIVO, supplier.getStatus());
    }

    @Test
    void addressToEntity_mapeiaCorretamente() {
        Address result = SupplierMapper.toEntity(addressDto);
        assertEquals("Rua do Fornecedor", result.getStreet());
        assertEquals("300", result.getNumber());
        assertEquals("Galpão 2", result.getComplement());
        assertEquals("Industrial", result.getNeighborhood());
        assertEquals("São Paulo", result.getCity());
        assertEquals("SP", result.getState());
        assertEquals("01310-300", result.getZipCode());
    }

    @Test
    void addressToDto_mapeiaCorretamente() {
        Address address = new Address();
        address.setStreet("Rua do Fornecedor");
        address.setNumber("300");
        address.setComplement("Galpão 2");
        address.setNeighborhood("Industrial");
        address.setCity("São Paulo");
        address.setState("SP");
        address.setZipCode("01310-300");

        AddressDto result = SupplierMapper.toDto(address);
        assertEquals("Rua do Fornecedor", result.street());
        assertEquals("300", result.number());
        assertEquals("Galpão 2", result.complement());
        assertEquals("Industrial", result.neighborhood());
        assertEquals("São Paulo", result.city());
        assertEquals("SP", result.state());
        assertEquals("01310-300", result.zipCode());
    }
}
