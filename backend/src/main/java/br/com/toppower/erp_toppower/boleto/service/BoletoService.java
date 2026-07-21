package br.com.toppower.erp_toppower.boleto.service;

import br.com.toppower.erp_toppower.boleto.dto.BoletoCreateRequest;
import br.com.toppower.erp_toppower.boleto.dto.BoletoResponse;
import br.com.toppower.erp_toppower.boleto.dto.BoletoUpdateRequest;
import br.com.toppower.erp_toppower.boleto.entity.Boleto;
import br.com.toppower.erp_toppower.boleto.exception.BoletoNotFoundException;
import br.com.toppower.erp_toppower.boleto.exception.DuplicateBoletoDescriptionException;
import br.com.toppower.erp_toppower.boleto.mapper.BoletoMapper;
import br.com.toppower.erp_toppower.boleto.repository.BoletoRepository;
import br.com.toppower.erp_toppower.common.dto.PagedResponse;
import br.com.toppower.erp_toppower.common.enums.RegistrationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BoletoService {

    private static final int MIN_SEARCH_QUERY_LENGTH = 2;

    private final BoletoRepository boletoRepository;

    public BoletoService(BoletoRepository boletoRepository) {
        this.boletoRepository = boletoRepository;
    }

    @Transactional
    public BoletoResponse create(BoletoCreateRequest request) {
        if (boletoRepository.existsByDescription(request.description())) {
            throw new DuplicateBoletoDescriptionException(request.description());
        }
        Boleto boleto = BoletoMapper.toEntity(request);
        Boleto saved = boletoRepository.save(boleto);
        return BoletoMapper.toResponse(saved);
    }

    /**
     * Lista paginada de boletos. Se {@code status} for nulo, retorna todos
     * (ativos e inativas); caso contrário filtra pelo status informado.
     */
    @Transactional(readOnly = true)
    public PagedResponse<BoletoResponse> getAll(RegistrationStatus status, Pageable pageable) {
        Page<Boleto> page = (status == null)
                ? boletoRepository.findAll(pageable)
                : boletoRepository.findByStatus(status, pageable);
        Page<BoletoResponse> mapped = page.map(BoletoMapper::toResponse);
        return PagedResponse.from(mapped);
    }

    @Transactional(readOnly = true)
    public BoletoResponse getById(Long id) {
        return boletoRepository.findById(id)
                .map(BoletoMapper::toResponse)
                .orElseThrow(() -> new BoletoNotFoundException(id));
    }

    /**
     * Busca flexível por texto (opcional) e/ou status (opcional).
     * <ul>
     *   <li>Apenas {@code status} → lista todos os boletos com aquele status</li>
     *   <li>Apenas {@code query} → lista todos os boletos que dão match com o texto</li>
     *   <li>Ambos → lista todos os boletos com aquele status E que dão match com o texto</li>
     *   <li>Nenhum → lista todos os boletos (paginado)</li>
     * </ul>
     * Quando {@code query} é informado, exige no mínimo 2 caracteres.
     */
    @Transactional(readOnly = true)
    public PagedResponse<BoletoResponse> search(String query, RegistrationStatus status, Pageable pageable) {
        String trimmed = (query == null) ? null : query.trim();
        if (trimmed != null && !trimmed.isEmpty() && trimmed.length() < MIN_SEARCH_QUERY_LENGTH) {
            throw new IllegalArgumentException(
                    "O termo de busca deve ter ao menos " + MIN_SEARCH_QUERY_LENGTH + " caracteres");
        }
        Page<BoletoResponse> mapped = boletoRepository
                .searchByQuery(status, trimmed, pageable)
                .map(BoletoMapper::toResponse);
        return PagedResponse.from(mapped);
    }

    @Transactional
    public BoletoResponse update(Long id, BoletoUpdateRequest request) {
        Boleto boleto = boletoRepository.findById(id)
                .orElseThrow(() -> new BoletoNotFoundException(id));

        // Se está alterando a descrição, valida duplicidade.
        if (request.description() != null
                && !request.description().equalsIgnoreCase(boleto.getDescription())
                && boletoRepository.existsByDescription(request.description())) {
            throw new DuplicateBoletoDescriptionException(request.description());
        }

        BoletoMapper.applyUpdate(boleto, request);
        Boleto saved = boletoRepository.save(boleto);
        return BoletoMapper.toResponse(saved);
    }

    /**
     * Soft delete: não remove fisicamente o registro, apenas altera o status para INATIVO.
     * Preserva o histórico de auditoria.
     */
    @Transactional
    public void softDelete(Long id) {
        Boleto boleto = boletoRepository.findById(id)
                .orElseThrow(() -> new BoletoNotFoundException(id));
        boleto.setStatus(RegistrationStatus.INATIVO);
        boletoRepository.save(boleto);
    }

    /**
     * Reativa um boleto inativo, alterando o status para ATIVO.
     */
    @Transactional
    public BoletoResponse activate(Long id) {
        Boleto boleto = boletoRepository.findById(id)
                .orElseThrow(() -> new BoletoNotFoundException(id));
        boleto.setStatus(RegistrationStatus.ATIVO);
        Boleto saved = boletoRepository.save(boleto);
        return BoletoMapper.toResponse(saved);
    }
}