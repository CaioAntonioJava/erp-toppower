package br.com.toppower.erp_toppower.customer.mapper;

import br.com.toppower.erp_toppower.common.dto.AddressDto;
import br.com.toppower.erp_toppower.common.embeddable.Address;
import br.com.toppower.erp_toppower.customer.dto.CustomerCreateRequest;
import br.com.toppower.erp_toppower.customer.dto.CustomerResponse;
import br.com.toppower.erp_toppower.customer.dto.CustomerUpdateRequest;
import br.com.toppower.erp_toppower.customer.entity.Customer;

public final class CustomerMapper {

    private CustomerMapper() {
    }

    public static Address toEntity(AddressDto dto) {
        Address address = new Address();
        address.setStreet(dto.street());
        address.setNumber(dto.number());
        address.setComplement(dto.complement());
        address.setNeighborhood(dto.neighborhood());
        address.setCity(dto.city());
        address.setState(dto.state());
        address.setZipCode(dto.zipCode());
        return address;
    }

    public static AddressDto toDto(Address address) {
        return new AddressDto(
                address.getStreet(),
                address.getNumber(),
                address.getComplement(),
                address.getNeighborhood(),
                address.getCity(),
                address.getState(),
                address.getZipCode()
        );
    }

    /**
     * Cria uma nova entidade a partir do request de criação.
     * <ul>
     *   <li>O {@code code} (código interno) NÃO é setado aqui — ele é gerado
     *   automaticamente no service (ex.: {@code CLI000001}) antes do
     *   {@code save}.</li>
     *   <li>O {@code status} pode ser {@code null}; o {@code @PrePersist} da
     *   entidade cuida de aplicar o default {@code ATIVO}.</li>
     * </ul>
     */
    public static Customer toEntity(CustomerCreateRequest request) {
        Customer customer = new Customer();
        customer.setName(request.name());
        customer.setEmail(request.email());
        customer.setPhone(request.phone());
        customer.setCpf(request.cpf());
        customer.setAddress(toEntity(request.address()));
        customer.setStatus(request.status());
        return customer;
    }

    public static CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getCpf(),
                customer.getCode(),
                toDto(customer.getAddress()),
                customer.getStatus(),
                customer.getCreatedAt(),
                customer.getUpdatedAt(),
                customer.getCreatedBy(),
                customer.getUpdatedBy()
        );
    }

    /**
     * Aplica uma atualização parcial (PATCH) na entidade carregada.
     * Apenas campos não nulos do request sobrescrevem o estado atual.
     * <ul>
     *   <li>O CPF NÃO é alterável (identidade fiscal).</li>
     *   <li>O {@code code} NÃO é alterável — é gerado uma única vez no
     *   momento do cadastro e nunca muda.</li>
     * </ul>
     */
    public static void applyUpdate(Customer customer, CustomerUpdateRequest request) {
        if (request.name() != null) {
            customer.setName(request.name());
        }
        if (request.email() != null) {
            customer.setEmail(request.email());
        }
        if (request.phone() != null) {
            customer.setPhone(request.phone());
        }
        if (request.cpf() != null) {
            customer.setCpf(request.cpf());
        }
        if (request.address() != null) {
            customer.setAddress(toEntity(request.address()));
        }
        if (request.status() != null) {
            customer.setStatus(request.status());
        }
    }
}
