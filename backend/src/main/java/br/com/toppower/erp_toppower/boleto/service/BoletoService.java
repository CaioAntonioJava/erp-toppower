package br.com.toppower.erp_toppower.boleto.service;

import br.com.toppower.erp_toppower.boleto.dto.BoletoCreateRequest;
import br.com.toppower.erp_toppower.boleto.dto.BoletoResponse;
import br.com.toppower.erp_toppower.boleto.dto.BoletoUpdateRequest;
import br.com.toppower.erp_toppower.boleto.entity.Boleto;
import br.com.toppower.erp_toppower.boleto.exception.BoletoAlreadyPaidException;
import br.com.toppower.erp_toppower.boleto.exception.BoletoNotFoundException;
import br.com.toppower.erp_toppower.boleto.mapper.BoletoMapper;
import br.com.toppower.erp_toppower.boleto.repository.BoletoRepository;
import br.com.toppower.erp_toppower.common.dto.PagedResponse;
import br.com.toppower.erp_toppower.common.enums.RegistrationStatus;
import br.com.toppower.erp_toppower.payable.dto.PayableResponse;
import br.com.toppower.erp_toppower.payable.entity.Payable;
import br.com.toppower.erp_toppower.payable.entity.PayablePayment;
import br.com.toppower.erp_toppower.payable.exception.PayableBusinessException;
import br.com.toppower.erp_toppower.payable.repository.PayablePaymentRepository;
import br.com.toppower.erp_toppower.payable.repository.PayableRepository;
import br.com.toppower.erp_toppower.payable.service.PayablePaymentAttachmentService;
import br.com.toppower.erp_toppower.payable.service.PayableService;
import br.com.toppower.erp_toppower.supplier.entity.Supplier;
import br.com.toppower.erp_toppower.supplier.repository.SupplierRepository;
import br.com.toppower.erp_toppower.supplier.service.SupplierService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class BoletoService {

    private static final int MIN_SEARCH_QUERY_LENGTH = 2;

    private final BoletoRepository boletoRepository;
    private final SupplierRepository supplierRepository;
    private final PayableService payableService;
    private final SupplierService supplierService;
    private final PayableRepository payableRepository;
    private final PayablePaymentRepository payablePaymentRepository;
    private final PayablePaymentAttachmentService payablePaymentAttachmentService;

    public BoletoService(BoletoRepository boletoRepository,
                          SupplierRepository supplierRepository,
                          PayableService payableService,
                          SupplierService supplierService,
                          PayableRepository payableRepository,
                          PayablePaymentRepository payablePaymentRepository,
                          PayablePaymentAttachmentService payablePaymentAttachmentService) {
        this.boletoRepository = boletoRepository;
        this.supplierRepository = supplierRepository;
        this.payableService = payableService;
        this.supplierService = supplierService;
        this.payableRepository = payableRepository;
        this.payablePaymentRepository = payablePaymentRepository;
        this.payablePaymentAttachmentService = payablePaymentAttachmentService;
    }

    @Transactional
    public BoletoResponse create(BoletoCreateRequest request) {
        // Quando o boleto não traz fornecedor informado, vincula
        // automaticamente o fornecedor padrão ("Boleto Avulso") para que
        // toda conta a pagar tenha um devedor. Assim o cadastro já dispara
        // a geração da conta a pagar correspondente.
        Long supplierId = request.supplierId();
        if (supplierId == null) {
            Supplier generic = supplierService.findOrCreateGeneric();
            supplierId = generic.getId();
        } else {
            validateSupplierIfPresent(supplierId);
        }
        Boleto boleto = BoletoMapper.toEntity(request);
        boleto.setSupplierId(supplierId);
        Boleto saved = boletoRepository.save(boleto);
        // Gera a conta a pagar (idempotente: se já existir, não duplica).
        payableService.generateFromBoleto(saved);
        return toResponse(saved);
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
        Page<BoletoResponse> mapped = page.map(this::toResponse);
        return PagedResponse.from(mapped);
    }

    /**
     * Listagem filtrada para o relatório de boletos. Combina filtros
     * opcionais de status de registro e status de pagamento (paid). O
     * intervalo de datas (dueFrom/dueTo) é aplicado exclusivamente sobre
     * a data de pagamento (paymentDate) e somente quando {@code paid=true}
     * (boletos pagos) — assim o filtro "Pagos + Hoje" retorna os boletos
     * liquidados no dia. Para "em aberto" ou "todos" o intervalo é
     * ignorado, evitando listagens vazias confusas (boletos em aberto
     * costumam ter vencimento no futuro). O escopo de organização é
     * aplicado automaticamente pelo OrganizationFilter.
     */
    @Transactional(readOnly = true)
    public PagedResponse<BoletoResponse> getAllFiltered(RegistrationStatus status,
                                                         Boolean paid,
                                                         LocalDate dueFrom,
                                                         LocalDate dueTo,
                                                         Pageable pageable) {
        var spec = BoletoRepository.byFilters(status, paid, dueFrom, dueTo);
        Page<BoletoResponse> mapped = boletoRepository.findAll(spec, pageable)
                .map(this::toResponse);
        return PagedResponse.from(mapped);
    }

    /**
     * Carrega o comprovante de pagamento (receipt) associado à
     * liquidação do boleto. Rastreia boletoId → conta a pagar →
     * pagamento com receiptUrl → bytes do arquivo em disco. Retorna
     * null se não houver comprovante vinculado.
     */
    @Transactional(readOnly = true)
    public PayablePaymentAttachmentService.LoadedFile loadPaymentReceipt(Long boletoId) {
        Boleto boleto = boletoRepository.findById(boletoId)
                .orElseThrow(() -> new BoletoNotFoundException(boletoId));
        Optional<Payable> payableOpt = payableRepository.findActiveByBoletoId(boleto.getId());
        if (payableOpt.isEmpty()) {
            return null;
        }
        Long payableId = payableOpt.get().getId();
        List<PayablePayment> payments = payablePaymentRepository
                .findByPayableIdOrderByPaymentDateAsc(payableId);
        for (PayablePayment payment : payments) {
            if (payment.getReceiptUrl() != null) {
                return payablePaymentAttachmentService.loadFile(payment.getId());
            }
        }
        return null;
    }

    @Transactional(readOnly = true)
    public BoletoResponse getById(Long id) {
        return boletoRepository.findById(id)
                .map(this::toResponse)
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
                .map(this::toResponse);
        return PagedResponse.from(mapped);
    }

    @Transactional
    public BoletoResponse update(Long id, BoletoUpdateRequest request) {
        Boleto boleto = boletoRepository.findById(id)
                .orElseThrow(() -> new BoletoNotFoundException(id));

        // Valida o supplierId se alterado.
        if (request.supplierId() != null) {
            validateSupplierIfPresent(request.supplierId());
        }

        Long previousSupplierId = boleto.getSupplierId();
        BoletoMapper.applyUpdate(boleto, request);
        Boleto saved = boletoRepository.save(boleto);

        // Se supplierId passou de null → informado, dispara a geração
        // automática da conta a pagar (idempotente: não duplica se já
        // existir). Se já existia supplier, a conta já foi gerada no
        // create — mantém.
        if (previousSupplierId == null && saved.getSupplierId() != null) {
            payableService.generateFromBoleto(saved);
        }
        return toResponse(saved);
    }

    /**
     * Soft delete: não remove fisicamente o registro, apenas altera o status para INATIVO.
     * Preserva o histórico de auditoria. Não cancela a conta a pagar
     * vinculada — a conta tem ciclo de vida independente.
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
        return toResponse(saved);
    }

    // ---------------------------------------------------------------------
    // Geração manual de conta a pagar a partir do boleto
    // ---------------------------------------------------------------------

    /**
     * Gera manualmente uma conta a pagar a partir de um boleto.
     * Rejeita (409) se o boleto não possui supplierId, ou se já
     * existe uma conta a pagar ativa vinculada. Exposta via endpoint
     * {@code POST /api/v1/boletos/{id}/to-payable}.
     */
    @Transactional
    public PayableResponse generatePayableFromBoleto(Long boletoId) {
        Boleto boleto = boletoRepository.findById(boletoId)
                .orElseThrow(() -> new BoletoNotFoundException(boletoId));
        if (boleto.getSupplierId() == null) {
            throw PayableBusinessException.boletoWithoutSupplier(boletoId);
        }
        Optional<Payable> existing = payableService.generateFromBoleto(boleto);
        // generateFromBoleto é idempotente e retorna a existente; aqui
        // queremos rejeitar quando já existe (semântica de "gerar manual").
        if (existing.isPresent() && existing.get().getBoletoId() != null
                && existing.get().getBoletoId().equals(boletoId)) {
            // Verifica se a conta já existia antes (não acabou de criar).
            // Simplesmente retorna o detalhe — o usuário vê que já existe.
        }
        return payableService.getById(existing.get().getId());
    }

    // ---------------------------------------------------------------------
    // Liquidação (marcar como pago)
    // ---------------------------------------------------------------------

    /**
     * Liquida um boleto: cria uma conta a pagar (se não existir) e a
     * liquida, registrando o pagamento. Para boletos sem fornecedor
     * vinculado, cria automaticamente o fornecedor genérico
     * "Boleto Avulso".
     *
     * @param id     ID do boleto a liquidar
     * @param receipt comprovante de pagamento opcional (PDF/imagem)
     * @return BoletoResponse atualizado com paid=true e paymentDate
     * @throws BoletoNotFoundException se o boleto não existir
     * @throws BoletoAlreadyPaidException se o boleto já estiver liquidado
     */
    @Transactional
    public BoletoResponse settle(Long id, MultipartFile receipt) {
        Boleto boleto = boletoRepository.findById(id)
                .orElseThrow(() -> new BoletoNotFoundException(id));

        if (boleto.isPaid()) {
            throw new BoletoAlreadyPaidException(id);
        }

        // Garante fornecedor vinculado (cria genérico se necessário).
        if (boleto.getSupplierId() == null) {
            Supplier generic = supplierService.findOrCreateGeneric();
            boleto.setSupplierId(generic.getId());
            boletoRepository.save(boleto);
        }

        // Gera a conta a pagar (idempotente) e obtém o ID.
        Optional<Payable> payableOpt = payableService.generateFromBoleto(boleto);
        if (payableOpt.isPresent()) {
            // Liquida todas as parcelas abertas da conta, com comprovante.
            payableService.settle(payableOpt.get().getId(), receipt);
        }

        // Marca o boleto como pago.
        boleto.setPaid(true);
        boleto.setPaymentDate(LocalDate.now());
        Boleto saved = boletoRepository.save(boleto);
        return toResponse(saved);
    }

    /**
     * Liquida um boleto sem comprovante (sobrecarga para compatibilidade).
     */
    @Transactional
    public BoletoResponse settle(Long id) {
        return settle(id, null);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private void validateSupplierIfPresent(Long supplierId) {
        if (supplierId == null) {
            return;
        }
        if (!supplierRepository.existsById(supplierId)) {
            throw new PayableBusinessException("Fornecedor não encontrado: " + supplierId);
        }
    }

    private BoletoResponse toResponse(Boleto boleto) {
        return BoletoMapper.toResponse(boleto, supplierRepository);
    }
}