package br.com.toppower.erp_toppower.customer.mapper;

import br.com.toppower.erp_toppower.common.dto.AddressDto;
import br.com.toppower.erp_toppower.common.embeddable.Address;
import br.com.toppower.erp_toppower.common.enums.RegistrationStatus;
import br.com.toppower.erp_toppower.customer.dto.CustomerCreateRequest;
import br.com.toppower.erp_toppower.customer.dto.CustomerUpdateRequest;
import br.com.toppower.erp_toppower.customer.entity.Customer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários de {@link CustomerMapper}.
 *
 * <p>Cobre toEntity, toResponse, applyUpdate e conversão de Address.</p>
 */
class CustomerMapperTest {

    private final AddressDto addressDto = new AddressDto(
            "Rua do Cliente", "200", "Apto 42", "Jardins", "São Paulo", "SP", "01310-200");

    @Test
    void toEntity_mapeiaCamposCorretamente() {
        CustomerCreateRequest request = new CustomerCreateRequest(
                "João Silva", "joao@email.com", "11999999999", "123.456.789-09",
                addressDto, RegistrationStatus.ATIVO);

        Customer result = CustomerMapper.toEntity(request);

        assertEquals("João Silva", result.getName());
        assertEquals("joao@email.com", result.getEmail());
        assertEquals("11999999999", result.getPhone());
        assertEquals("123.456.789-09", result.getCpf());
        assertEquals(RegistrationStatus.ATIVO, result.getStatus());
        assertNotNull(result.getAddress());
        assertEquals("Rua do Cliente", result.getAddress().getStreet());
    }

    @Test
    void toEntity_statusNulo_naoAplicaDefault() {
        CustomerCreateRequest request = new CustomerCreateRequest(
                "João", "joao@email.com", "11999999999", "123.456.789-09",
                addressDto, null);
        Customer result = CustomerMapper.toEntity(request);
        assertNull(result.getStatus());
    }

    @Test
    void toResponse_mapeiaCamposCorretamente() {
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setName("Maria Souza");
        customer.setEmail("maria@email.com");
        customer.setPhone("11888888888");
        customer.setCpf("529.982.247-25");
        customer.setCode("CLI000001");
        Address addr = new Address();
        addr.setStreet("Rua B");
        addr.setNumber("300");
        addr.setCity("Rio de Janeiro");
        addr.setState("RJ");
        customer.setAddress(addr);
        customer.setStatus(RegistrationStatus.ATIVO);

        var response = CustomerMapper.toResponse(customer);

        assertEquals(1L, response.id());
        assertEquals("Maria Souza", response.name());
        assertEquals("maria@email.com", response.email());
        assertEquals("CLI000001", response.code());
        assertEquals("Rua B", response.address().street());
        assertEquals(RegistrationStatus.ATIVO, response.status());
    }

    @Test
    void applyUpdate_camposNaoNulos_atualiza() {
        Customer customer = new Customer();
        customer.setName("Original");
        customer.setEmail("original@email.com");
        customer.setPhone("11111111111");
        customer.setCpf("123.456.789-09");
        Address addr = new Address();
        addr.setStreet("Rua Original");
        addr.setNumber("1");
        addr.setCity("São Paulo");
        addr.setState("SP");
        customer.setAddress(addr);
        customer.setStatus(RegistrationStatus.ATIVO);

        AddressDto newAddress = new AddressDto("Rua Nova", "500", null, "Centro", "Belo Horizonte", "MG", "30000-000");
        CustomerUpdateRequest update = new CustomerUpdateRequest(
                "Novo Nome", "novo@email.com", "22222222222", "529.982.247-25",
                newAddress, RegistrationStatus.INATIVO);

        CustomerMapper.applyUpdate(customer, update);

        assertEquals("Novo Nome", customer.getName());
        assertEquals("novo@email.com", customer.getEmail());
        assertEquals("22222222222", customer.getPhone());
        assertEquals("529.982.247-25", customer.getCpf());
        assertEquals("Rua Nova", customer.getAddress().getStreet());
        assertEquals(RegistrationStatus.INATIVO, customer.getStatus());
    }

    @Test
    void applyUpdate_camposNulos_naoAltera() {
        Customer customer = new Customer();
        customer.setName("Original");
        customer.setStatus(RegistrationStatus.ATIVO);

        CustomerUpdateRequest update = new CustomerUpdateRequest(
                null, null, null, null, null, null);

        CustomerMapper.applyUpdate(customer, update);

        assertEquals("Original", customer.getName());
        assertEquals(RegistrationStatus.ATIVO, customer.getStatus());
    }

    @Test
    void addressToEntity_mapeiaCorretamente() {
        Address result = CustomerMapper.toEntity(addressDto);
        assertEquals("Rua do Cliente", result.getStreet());
        assertEquals("200", result.getNumber());
        assertEquals("Apto 42", result.getComplement());
        assertEquals("Jardins", result.getNeighborhood());
        assertEquals("São Paulo", result.getCity());
        assertEquals("SP", result.getState());
        assertEquals("01310-200", result.getZipCode());
    }

    @Test
    void addressToDto_mapeiaCorretamente() {
        Address address = new Address();
        address.setStreet("Rua do Cliente");
        address.setNumber("200");
        address.setComplement("Apto 42");
        address.setNeighborhood("Jardins");
        address.setCity("São Paulo");
        address.setState("SP");
        address.setZipCode("01310-200");

        AddressDto result = CustomerMapper.toDto(address);
        assertEquals("Rua do Cliente", result.street());
        assertEquals("200", result.number());
        assertEquals("Apto 42", result.complement());
        assertEquals("Jardins", result.neighborhood());
        assertEquals("São Paulo", result.city());
        assertEquals("SP", result.state());
        assertEquals("01310-200", result.zipCode());
    }
}
