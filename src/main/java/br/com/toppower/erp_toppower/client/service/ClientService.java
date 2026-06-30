package br.com.toppower.erp_toppower.client.service;

import br.com.toppower.erp_toppower.client.dto.ClientCreateRequest;
import br.com.toppower.erp_toppower.client.dto.ClientResponse;
import br.com.toppower.erp_toppower.client.dto.ClientUpdateRequest;
import br.com.toppower.erp_toppower.client.entity.Client;
import br.com.toppower.erp_toppower.client.enums.ClientStatus;
import br.com.toppower.erp_toppower.client.exception.ClientNotFoundException;
import br.com.toppower.erp_toppower.client.exception.DuplicateClientCodeException;
import br.com.toppower.erp_toppower.client.exception.DuplicateClientTaxIdException;
import br.com.toppower.erp_toppower.client.mapper.ClientMapper;
import br.com.toppower.erp_toppower.client.repository.ClientRepository;
import br.com.toppower.erp_toppower.common.dto.PagedResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ClientService {

    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Transactional
    public ClientResponse create(ClientCreateRequest request) {
        if (clientRepository.existsByCode(request.code())) {
            throw new DuplicateClientCodeException(request.code());
        }
        if (clientRepository.existsByTaxId(request.taxId())) {
            throw new DuplicateClientTaxIdException(request.taxId());
        }
        Client client = ClientMapper.toEntity(request);
        Client saved = clientRepository.save(client);
        return ClientMapper.toResponse(saved);
    }

    /**
     * Lista paginada de clientes. Se {@code status} for nulo, retorna todos
     * (ativos e inativos); caso contrário filtra pelo status informado.
     */
    @Transactional(readOnly = true)
    public PagedResponse<ClientResponse> getAll(ClientStatus status, Pageable pageable) {
        Page<Client> page = (status == null)
                ? clientRepository.findAll(pageable)
                : clientRepository.findByStatus(status, pageable);
        Page<ClientResponse> mapped = page.map(ClientMapper::toResponse);
        return PagedResponse.from(mapped);
    }

    @Transactional(readOnly = true)
    public ClientResponse getById(UUID id) {
        return clientRepository.findById(id)
                .map(ClientMapper::toResponse)
                .orElseThrow(() -> new ClientNotFoundException(id));
    }

    private static final int MIN_SEARCH_QUERY_LENGTH = 2;

    /**
     * Busca textual por código, razão social ou nome fantasia, com filtro
     * opcional de status. A query deve ter no mínimo 2 caracteres.
     */
    @Transactional(readOnly = true)
    public PagedResponse<ClientResponse> search(String query, ClientStatus status, Pageable pageable) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("O termo de busca é obrigatório");
        }
        String trimmed = query.trim();
        if (trimmed.length() < MIN_SEARCH_QUERY_LENGTH) {
            throw new IllegalArgumentException(
                    "O termo de busca deve ter ao menos " + MIN_SEARCH_QUERY_LENGTH + " caracteres");
        }
        Page<ClientResponse> mapped = clientRepository
                .searchByQuery(status, trimmed, pageable)
                .map(ClientMapper::toResponse);
        return PagedResponse.from(mapped);
    }

    @Transactional
    public ClientResponse update(UUID id, ClientUpdateRequest request) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ClientNotFoundException(id));

        ClientMapper.applyUpdate(client, request);
        Client saved = clientRepository.save(client);
        return ClientMapper.toResponse(saved);
    }

    /**
     * Soft delete: não remove fisicamente o registro, apenas altera o status para INATIVO.
     * Preserva o histórico de auditoria e referências em pedidos/notas fiscais.
     */
    @Transactional
    public void softDelete(UUID id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ClientNotFoundException(id));
        client.setStatus(ClientStatus.INATIVO);
        clientRepository.save(client);
    }

    /**
     * Reativa um cliente inativo, alterando o status para ATIVO.
     */
    @Transactional
    public ClientResponse activate(UUID id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ClientNotFoundException(id));
        client.setStatus(ClientStatus.ATIVO);
        Client saved = clientRepository.save(client);
        return ClientMapper.toResponse(saved);
    }
}
