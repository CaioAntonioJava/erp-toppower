package br.com.toppower.erp_toppower.carrier.service;

import br.com.toppower.erp_toppower.carrier.dto.CarrierCreateRequest;
import br.com.toppower.erp_toppower.carrier.dto.CarrierResponse;
import br.com.toppower.erp_toppower.carrier.dto.CarrierUpdateRequest;
import br.com.toppower.erp_toppower.carrier.entity.Carrier;
import br.com.toppower.erp_toppower.carrier.enums.CarrierStatus;
import br.com.toppower.erp_toppower.carrier.exception.CarrierNotFoundException;
import br.com.toppower.erp_toppower.carrier.mapper.CarrierMapper;
import br.com.toppower.erp_toppower.carrier.repository.CarrierRepository;
import br.com.toppower.erp_toppower.common.dto.PagedResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CarrierService {

    private static final int MIN_SEARCH_QUERY_LENGTH = 2;

    private final CarrierRepository carrierRepository;

    public CarrierService(CarrierRepository carrierRepository) {
        this.carrierRepository = carrierRepository;
    }

    @Transactional
    public CarrierResponse create(CarrierCreateRequest request) {
        Carrier carrier = CarrierMapper.toEntity(request);
        Carrier saved = carrierRepository.save(carrier);
        return CarrierMapper.toResponse(saved);
    }

    /**
     * Lista paginada de transportadoras. Se {@code status} for nulo, retorna todos
     * (ativos e inativos); caso contrário filtra pelo status informado.
     */
    @Transactional(readOnly = true)
    public PagedResponse<CarrierResponse> getAll(CarrierStatus status, Pageable pageable) {
        Page<Carrier> page = (status == null)
                ? carrierRepository.findAll(pageable)
                : carrierRepository.findByStatus(status, pageable);
        Page<CarrierResponse> mapped = page.map(CarrierMapper::toResponse);
        return PagedResponse.from(mapped);
    }

    /**
     * Lista paginada apenas de transportadoras com status {@link CarrierStatus#ATIVO}.
     * Atalho semântico para {@code getAll(CarrierStatus.ATIVO, pageable)}.
     */
    @Transactional(readOnly = true)
    public PagedResponse<CarrierResponse> findActive(Pageable pageable) {
        return getAll(CarrierStatus.ATIVO, pageable);
    }

    /**
     * Lista paginada apenas de transportadoras com status {@link CarrierStatus#INATIVO}.
     * Atalho semântico para {@code getAll(CarrierStatus.INATIVO, pageable)}.
     */
    @Transactional(readOnly = true)
    public PagedResponse<CarrierResponse> findInactive(Pageable pageable) {
        return getAll(CarrierStatus.INATIVO, pageable);
    }

    @Transactional(readOnly = true)
    public CarrierResponse getById(UUID id) {
        return carrierRepository.findById(id)
                .map(CarrierMapper::toResponse)
                .orElseThrow(() -> new CarrierNotFoundException(id));
    }

    @Transactional
    public CarrierResponse update(UUID id, CarrierUpdateRequest request) {
        Carrier carrier = carrierRepository.findById(id)
                .orElseThrow(() -> new CarrierNotFoundException(id));
        CarrierMapper.applyUpdate(carrier, request);
        Carrier saved = carrierRepository.save(carrier);
        return CarrierMapper.toResponse(saved);
    }

    /**
     * Soft delete: não remove fisicamente o registro, apenas altera o status para INATIVO.
     * Preserva o histórico e a rastreabilidade do registro.
     */
    @Transactional
    public void softDelete(UUID id) {
        Carrier carrier = carrierRepository.findById(id)
                .orElseThrow(() -> new CarrierNotFoundException(id));
        carrier.setStatus(CarrierStatus.INATIVO);
        carrierRepository.save(carrier);
    }

    /**
     * Reativa uma transportadora inativa, alterando o status para ATIVO.
     */
    @Transactional
    public CarrierResponse activate(UUID id) {
        Carrier carrier = carrierRepository.findById(id)
                .orElseThrow(() -> new CarrierNotFoundException(id));
        carrier.setStatus(CarrierStatus.ATIVO);
        Carrier saved = carrierRepository.save(carrier);
        return CarrierMapper.toResponse(saved);
    }

    /**
     * Busca flexível por texto (opcional) e/ou status (opcional).
     * <ul>
     *   <li>Apenas {@code status} → lista todas as transportadoras com aquele status</li>
     *   <li>Apenas {@code query} → lista todas as transportadoras que dão match com o texto</li>
     *   <li>Ambos → lista as transportadoras com aquele status E que dão match</li>
     *   <li>Nenhum → lista todas as transportadoras (paginado)</li>
     * </ul>
     * Quando {@code query} é informado, exige no mínimo 2 caracteres.
     */
    @Transactional(readOnly = true)
    public PagedResponse<CarrierResponse> search(String query, CarrierStatus status, Pageable pageable) {
        String trimmed = (query == null) ? null : query.trim();
        if (trimmed != null && !trimmed.isEmpty() && trimmed.length() < MIN_SEARCH_QUERY_LENGTH) {
            throw new IllegalArgumentException(
                    "O termo de busca deve ter ao menos " + MIN_SEARCH_QUERY_LENGTH + " caracteres");
        }
        Page<CarrierResponse> mapped = carrierRepository
                .searchByQuery(status, trimmed, pageable)
                .map(CarrierMapper::toResponse);
        return PagedResponse.from(mapped);
    }
}
