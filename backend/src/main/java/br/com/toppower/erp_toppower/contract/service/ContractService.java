package br.com.toppower.erp_toppower.contract.service;

import br.com.toppower.erp_toppower.common.context.OrganizationContext;
import br.com.toppower.erp_toppower.common.dto.PagedResponse;
import br.com.toppower.erp_toppower.company.entity.Company;
import br.com.toppower.erp_toppower.company.repository.CompanyRepository;
import br.com.toppower.erp_toppower.contract.dto.ContractClauseRequest;
import br.com.toppower.erp_toppower.contract.dto.ContractCreateRequest;
import br.com.toppower.erp_toppower.contract.dto.ContractResponse;
import br.com.toppower.erp_toppower.contract.dto.ContractUpdateRequest;
import br.com.toppower.erp_toppower.contract.dto.NextContractCodeResponse;
import br.com.toppower.erp_toppower.contract.entity.Contract;
import br.com.toppower.erp_toppower.contract.entity.ContractClause;
import br.com.toppower.erp_toppower.contract.enums.ContractStatus;
import br.com.toppower.erp_toppower.contract.exception.ContractBusinessException;
import br.com.toppower.erp_toppower.contract.exception.ContractClientNotFoundException;
import br.com.toppower.erp_toppower.contract.exception.ContractNotFoundException;
import br.com.toppower.erp_toppower.contract.exception.InvalidContractClientException;
import br.com.toppower.erp_toppower.contract.mapper.ContractMapper;
import br.com.toppower.erp_toppower.contract.repository.ContractClauseRepository;
import br.com.toppower.erp_toppower.contract.repository.ContractRepository;
import br.com.toppower.erp_toppower.customer.entity.Customer;
import br.com.toppower.erp_toppower.customer.repository.CustomerRepository;
import br.com.toppower.erp_toppower.organization.entity.Organization;
import br.com.toppower.erp_toppower.organization.repository.OrganizationRepository;
import br.com.toppower.erp_toppower.servicetemplate.entity.ServiceTemplate;
import br.com.toppower.erp_toppower.servicetemplate.repository.ServiceTemplateRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Regras de negócio do ciclo de vida de um contrato.
 *
 * <p>Responsabilidades principais:</p>
 * <ul>
 *   <li>Gerar o código comercial ({@code <prefix>-<seq>-<year>}, ex.:
 *       {@code CL-001-2026} para Top Power Materiais ou
 *       {@code CT-001-2026} para Top Power Engenharia): prefixo lido da
 *       {@code Organization} ativa ({@code contract_prefix}), sequência
 *       reiniciando a {@code 1} a cada novo ano <b>por Organization</b>,
 *       ano corrente;</li>
 *   <li>Validar a invariante de cliente (exatamente um entre
 *       {@code customerId} e {@code companyId});</li>
 *   <li>Validar a existência do cliente referenciado;</li>
 *   <li>Pré-preencher o título como
 *       {@code "CONTRATO DE PRESTAÇÃO DE SERVIÇOS: <código>"} quando o
 *       cliente não o informar;</li>
 *   <li>Pré-preencher a descrição com o template padrão da Organization
 *       ({@code contract_default_description}) quando o cliente não a
 *       informar e a Organization tiver um template configurado;</li>
 *   <li>CRUD completo (create, getAll, getById, search, update);</li>
 *   <li>Soft delete (inativar) e reativação.</li>
 * </ul>
 */
@Service
public class ContractService {

    private static final int MIN_SEARCH_QUERY_LENGTH = 2;

    private final ContractRepository repository;
    private final ContractClauseRepository clauseRepository;
    private final OrganizationRepository organizationRepository;
    private final CustomerRepository customerRepository;
    private final CompanyRepository companyRepository;
    private final ServiceTemplateRepository serviceTemplateRepository;

    public ContractService(ContractRepository repository,
                           ContractClauseRepository clauseRepository,
                           OrganizationRepository organizationRepository,
                           CustomerRepository customerRepository,
                           CompanyRepository companyRepository,
                           ServiceTemplateRepository serviceTemplateRepository) {
        this.repository = repository;
        this.clauseRepository = clauseRepository;
        this.organizationRepository = organizationRepository;
        this.customerRepository = customerRepository;
        this.companyRepository = companyRepository;
        this.serviceTemplateRepository = serviceTemplateRepository;
    }

    // ---------------------------------------------------------------------
    // Create
    // ---------------------------------------------------------------------

    @Transactional
    public ContractResponse create(ContractCreateRequest request) {
        validateClientReference(request.customerId(), request.companyId(), true);

        Contract contract = ContractMapper.toEntity(request);
        applyNextCode(contract);
        applyDefaults(contract, request);
        Contract saved = repository.save(contract);

        // Cláusulas: se enviadas, persiste; senão, semeia as 11 padrão do PDF.
        if (request.clauses() != null && !request.clauses().isEmpty()) {
            persistClauses(request.clauses(), saved.getId());
        } else {
            seedDefaultClauses(saved.getId());
        }

        return toResponseWithClient(saved);
    }

    // ---------------------------------------------------------------------
    // Read
    // ---------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PagedResponse<ContractResponse> getAll(ContractStatus status, Pageable pageable) {
        Page<Contract> page = (status == null)
                ? repository.findAll(pageable)
                : repository.findByStatus(status, pageable);
        Page<ContractResponse> mapped = page.map(this::toResponseWithClient);
        return PagedResponse.from(mapped);
    }

    @Transactional(readOnly = true)
    public ContractResponse getById(Long id) {
        return repository.findById(id)
                .map(this::toResponseWithClient)
                .orElseThrow(() -> new ContractNotFoundException(id));
    }

    /**
     * Busca flexível por texto (opcional) e/ou status (opcional).
     * <ul>
     *   <li>Apenas {@code status} → lista todos os contratos com aquele status</li>
     *   <li>Apenas {@code query} → lista os contratos que dão match com o texto</li>
     *   <li>Ambos → lista os contratos com aquele status E que dão match</li>
     *   <li>Nenhum → lista todos (paginado)</li>
     * </ul>
     * Quando {@code query} é informado, exige no mínimo 2 caracteres.
     */
    @Transactional(readOnly = true)
    public PagedResponse<ContractResponse> search(String query, ContractStatus status, Pageable pageable) {
        String trimmed = (query == null) ? null : query.trim();
        if (trimmed != null && !trimmed.isEmpty() && trimmed.length() < MIN_SEARCH_QUERY_LENGTH) {
            throw new IllegalArgumentException(
                    "O termo de busca deve ter ao menos " + MIN_SEARCH_QUERY_LENGTH + " caracteres");
        }
        Page<ContractResponse> mapped = repository
                .searchByQuery(status, trimmed, pageable)
                .map(this::toResponseWithClient);
        return PagedResponse.from(mapped);
    }

    // ---------------------------------------------------------------------
    // Update
    // ---------------------------------------------------------------------

    @Transactional
    public ContractResponse update(Long id, ContractUpdateRequest request) {
        Contract contract = repository.findById(id)
                .orElseThrow(() -> new ContractNotFoundException(id));

        // Computa o cliente efetivo após o PATCH: se o request trouxe um
        // valor (inclusive null), usa o novo; senão mantém o existente.
        Long effectiveCustomer = (request.customerId() != null)
                ? request.customerId() : contract.getCustomerId();
        Long effectiveCompany = (request.companyId() != null)
                ? request.companyId() : contract.getCompanyId();
        validateClientReference(effectiveCustomer, effectiveCompany, false);

        ContractMapper.applyUpdate(contract, request);
        Contract saved = repository.save(contract);

        // Cláusulas: se o request trouxe a lista (inclusive vazia),
        // faz full replacement (delete-all + re-insert). Se omitida
        // (null), mantém as cláusulas atuais.
        if (request.clauses() != null) {
            clauseRepository.deleteByContractId(id);
            clauseRepository.flush();
            if (!request.clauses().isEmpty()) {
                persistClauses(request.clauses(), id);
            }
        }

        return toResponseWithClient(saved);
    }

    // ---------------------------------------------------------------------
    // Soft delete / Activate
    // ---------------------------------------------------------------------

    /**
     * Soft delete: não remove fisicamente o registro, apenas altera o
     * status para {@link ContractStatus#INATIVO}.
     */
    @Transactional
    public void softDelete(Long id) {
        Contract contract = repository.findById(id)
                .orElseThrow(() -> new ContractNotFoundException(id));
        contract.setStatus(ContractStatus.INATIVO);
        repository.save(contract);
    }

    /**
     * Reativa um contrato inativo, alterando o status para
     * {@link ContractStatus#ATIVO}.
     */
    @Transactional
    public ContractResponse activate(Long id) {
        Contract contract = repository.findById(id)
                .orElseThrow(() -> new ContractNotFoundException(id));
        contract.setStatus(ContractStatus.ATIVO);
        Contract saved = repository.save(contract);
        return toResponseWithClient(saved);
    }

    // ---------------------------------------------------------------------
    // Utilitários públicos
    // ---------------------------------------------------------------------

    /**
     * Retorna o código (e o título padrão) que seriam atribuídos ao
     * próximo contrato, sem persistir nada. Útil para o frontend exibir
     * o valor previsto no formulário antes do envio.
     *
     * <p>O prefixo vem da {@code Organization} ativa e a sequência é
     * computada de forma independente por Organization/ano.</p>
     */
    @Transactional(readOnly = true)
    public NextContractCodeResponse getNextCode() {
        Long orgId = OrganizationContext.require();
        int year = LocalDate.now().getYear();
        String prefix = currentOrgContractPrefix(orgId);
        long sequence = generateNextSequence(year, orgId);
        String code = prefix + "-" + formatSequence(sequence) + "-" + year;
        String defaultTitle = Contract.defaultTitle(prefix, sequence, year);
        LocalDate defaultValidityDate = LocalDate.now();

        // Carrega o template de descrição padrão da Organization ativa
        String defaultDescription = organizationRepository.findById(orgId)
                .map(Organization::getContractDefaultDescription)
                .filter(d -> d != null && !d.isBlank())
                .orElse(null);

        return new NextContractCodeResponse(prefix, sequence, year, code, defaultTitle,
                defaultValidityDate, defaultDescription);
    }

    // ---------------------------------------------------------------------
    // Helpers — geração do código
    // ---------------------------------------------------------------------

    /**
     * Aplica o próximo código disponível ao contrato: prefixo da
     * {@code Organization} ativa, sequência independente por
     * Organization/ano e ano corrente.
     */
    private void applyNextCode(Contract contract) {
        Long orgId = OrganizationContext.require();
        int year = LocalDate.now().getYear();
        long sequence = generateNextSequence(year, orgId);
        contract.setPrefix(currentOrgContractPrefix(orgId));
        contract.setSequence(sequence);
        contract.setYear(year);
    }

    private long generateNextSequence(int year, Long organizationId) {
        Long maxSequence = repository.findMaxSequenceByYearAndOrganizationId(year, organizationId);
        return (maxSequence == null) ? 1L : maxSequence + 1L;
    }

    /**
     * Resolve o prefixo de Contrato da {@code Organization} ativa
     * ({@link OrganizationContext}). Lança exceção de negócio quando a
     * Organization não tiver {@code contract_prefix} configurado.
     */
    private String currentOrgContractPrefix(Long orgId) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ContractBusinessException(
                        "Organization ativa não encontrada: " + orgId));
        String prefix = org.getContractPrefix();
        if (prefix == null || prefix.isBlank()) {
            throw new ContractBusinessException(
                    "Organization ativa (" + org.getTradeName() + ") não possui "
                            + "contract_prefix configurado. Atualize o cadastro da "
                            + "empresa antes de emitir contratos.");
        }
        return prefix;
    }

    private static String formatSequence(long sequence) {
        return String.format("%03d", sequence);
    }

    // ---------------------------------------------------------------------
    // Helpers — defaults do backend
    // ---------------------------------------------------------------------

    /**
     * Aplica os defaults do backend quando o request não os trouxer:
     * <ul>
     *   <li>{@code title}: pré-preenchido com
     *       {@code "CONTRATO DE PRESTAÇÃO DE SERVIÇOS: <código>"};</li>
     *   <li>{@code description}: pré-preenchida com o template padrão da
     *       Organization ativa ({@code contract_default_description}),
     *       quando disponível.</li>
     * </ul>
     * Não sobrescreve valores enviados pelo cliente.
     */
    private void applyDefaults(Contract contract, ContractCreateRequest request) {
        if (request.title() == null || request.title().isBlank()) {
            contract.setTitle(Contract.defaultTitle(
                    contract.getPrefix(), contract.getSequence(), contract.getYear()));
        }
        if ((request.description() == null || request.description().isBlank())) {
            Long orgId = OrganizationContext.require();
            Organization org = organizationRepository.findById(orgId).orElse(null);
            if (org != null
                    && org.getContractDefaultDescription() != null
                    && !org.getContractDefaultDescription().isBlank()) {
                contract.setDescription(org.getContractDefaultDescription());
            }
        }
    }

    // ---------------------------------------------------------------------
    // Helpers — validação e resolução de cliente
    // ---------------------------------------------------------------------

    /**
     * Valida a invariante de cliente: exatamente um entre
     * {@code customerId} e {@code companyId} deve ser não nulo.
     *
     * @param verifyExists quando {@code true}, também verifica se o
     *                     registro referenciado existe no banco
     */
    private void validateClientReference(Long customerId, Long companyId, boolean verifyExists) {
        if (customerId == null && companyId == null) {
            throw InvalidContractClientException.bothNull();
        }
        if (customerId != null && companyId != null) {
            throw InvalidContractClientException.bothSet();
        }
        if (!verifyExists) return;

        if (customerId != null) {
            if (!customerRepository.existsById(customerId)) {
                throw new ContractClientNotFoundException("CUSTOMER", customerId);
            }
        } else {
            if (!companyRepository.existsById(companyId)) {
                throw new ContractClientNotFoundException("COMPANY", companyId);
            }
        }
    }

    /**
     * Resolve os dados de exibição do cliente (nome e código) para
     * compor a resposta.
     */
    private ClientResolved resolveClient(Contract contract) {
        if (contract.getCustomerId() != null) {
            return customerRepository.findById(contract.getCustomerId())
                    .map(c -> new ClientResolved(c.getName(), c.getCode()))
                    .orElse(ClientResolved.EMPTY);
        }
        if (contract.getCompanyId() != null) {
            return companyRepository.findById(contract.getCompanyId())
                    .map(c -> {
                        String name = (c.getTradeName() != null && !c.getTradeName().isBlank())
                                ? c.getTradeName() : c.getLegalName();
                        return new ClientResolved(name, c.getCode());
                    })
                    .orElse(ClientResolved.EMPTY);
        }
        return ClientResolved.EMPTY;
    }

    private ContractResponse toResponseWithClient(Contract contract) {
        ClientResolved client = resolveClient(contract);
        List<ContractClause> clauses = clauseRepository
                .findByContractIdOrderByClauseNumberAsc(contract.getId());
        return ContractMapper.toResponse(contract, client.name(), client.code(), clauses);
    }

    // ---------------------------------------------------------------------
    // Helpers — cláusulas
    // ---------------------------------------------------------------------

    /**
     * Persiste a lista de cláusulas enviadas pelo cliente. Para a cláusula 1
     * (DO OBJETO), quando {@code serviceTemplateId} é informado, busca o
     * ServiceTemplate e copia sua descrição para o conteúdo da cláusula
     * (snapshot), mantendo o ID para rastreabilidade.
     */
    private void persistClauses(List<ContractClauseRequest> clauses, Long contractId) {
        for (ContractClauseRequest req : clauses) {
            ContractClause clause = ContractMapper.toClauseEntity(req, contractId);

            // Cláusula 1 com ServiceTemplate: copia a descrição do template.
            if (req.clauseNumber() != null
                    && req.clauseNumber() == 1
                    && req.serviceTemplateId() != null) {
                serviceTemplateRepository.findById(req.serviceTemplateId())
                        .ifPresent(template -> clause.setContent(template.getDescription()));
            }

            clauseRepository.save(clause);
        }
    }

    /**
     * Semeia as 11 cláusulas padrão extraídas do contrato modelo Top Power
     * (mão de obra). A cláusula 1 (DO OBJETO) é criada vazia — o usuário
     * deve selecionar um ServiceTemplate no frontend. As cláusulas 2–11
     * vêm pré-preenchidas com os textos do documento de referência.
     */
    private void seedDefaultClauses(Long contractId) {
        List<ContractClause> defaults = buildDefaultClauses(contractId);
        for (ContractClause clause : defaults) {
            clauseRepository.save(clause);
        }
    }

    /**
     * Constrói a lista de 11 cláusulas padrão (cláusula 1 vazia, 2–11
     * com os textos do contrato modelo). Usado tanto no seed de criação
     * quanto pode ser exposto via getNextCode para o frontend pré-preencher.
     */
    private List<ContractClause> buildDefaultClauses(Long contractId) {
        List<ContractClause> clauses = new ArrayList<>();

        clauses.add(newClause(contractId, 1, "CLÁUSULA PRIMEIRA - DO OBJETO", null, null));

        clauses.add(newClause(contractId, 2, "CLÁUSULA SEGUNDA - RESPONSABILIDADE DA CONTRATADA",
                "<p>2.1. Fornecimento de mão-de-obra especializada, ferramental, roupa e equipamentos "
                        + "para o bom desempenho dos trabalhos;</p>"
                        + "<p>2.2. Fornecimento de transporte e alimentação apropriado para os funcionários;</p>"
                        + "<p>2.3. Suporte Técnico Engenheiro com registro ativo no CREA, para supervisão;</p>"
                        + "<p>2.4. Sigilo sobre as atividades da BERNARDI HORTO EMPREENDIMENTOS IMOBILIARIOS "
                        + "SPE LTDA.</p>",
                null));

        clauses.add(newClause(contractId, 3, "CLÁUSULA TERCEIRA - RESPONSABILIDADE DA CONTRATANTE",
                "<p>3.1. Liberação da área de trabalho, em condições de desenvolver seus serviços em tempo; "
                        + "hábil para o cumprimento do prazo de execução previsto.</p>"
                        + "<p>3.2. Fornecimento de documentação técnica.</p>",
                null));

        clauses.add(newClause(contractId, 4, "CLÁUSULA QUARTA - DO PRAZO DE ENTREGA",
                "<p>4.1. 120 (CENTO E VINTE DIAS) a partir da autorização e serviço.</p>",
                null));

        clauses.add(newClause(contractId, 5, "CLÁUSULA QUINTA - DOS PREÇOS E FORMA DE PAGAMENTO",
                "<p>5.1. A CONTRATANTE pagará ao CONTRATADO, pelos serviços o valor total de "
                        + "R$ 230.800,00 (duzentos e trinta mil, oitocentos reais).</p>"
                        + "<p>5.2. Pagamento/Parcelas:</p>"
                        + "<p>. 15% na assinatura contrato – R$ 34.620,00</p>"
                        + "<p>. 20% 30 DDL - R$ 46.160,00</p>"
                        + "<p>. 20% 60 DDL - R$ 46.160,00</p>"
                        + "<p>. 25% 90 DDL - R$ 57.700,00</p>"
                        + "<p>. 20% 10 DDL após a finalização da obra - R$ 46.160,00</p>"
                        + "<p>5.3. Dados para transferência bancária</p>"
                        + "<p>Banco do Brasil</p>"
                        + "<p>Agencia 990-3</p>"
                        + "<p>Conta corrente 117.254-9</p>"
                        + "<p>5.4. No valor citado na clausula quinta estão inclusas as despesas com impostos "
                        + "e encargos sociais pertinentes a este contrato. Estamos considerando o "
                        + "recolhimento da ART (Anotação de Responsabilidade Técnica) para a execução dos "
                        + "itens objetos desta proposta.</p>",
                null));

        clauses.add(newClause(contractId, 6, "CLÁUSULA SEXTA - DA VIGÊNCIA",
                "<p>6.1. O presente Contrato vigorará durante o período necessário para a elaboração dos "
                        + "serviços descritos na Cláusula Primeira, limitado ao prazo estabelecido na "
                        + "Cláusula Segunda.</p>",
                null));

        clauses.add(newClause(contractId, 7, "CLÁUSULA SETIMA - DA RESCISÃO",
                "<p>7.1. Será motivo para rescisão imediata deste contrato o descumprimento de quaisquer de "
                        + "suas cláusulas, devendo a parte infratora arcar com as perdas e danos decorrentes "
                        + "do fato, honorários advocatícios e demais cominações legais.</p>",
                null));

        clauses.add(newClause(contractId, 8, "CLÁUSULA OITAVA - DA MULTA",
                "<p>8.1. Caso alguma das partes não cumpra o disposto nas cláusulas estabelecidas neste "
                        + "instrumento, responsabilizar-se-á pelo pagamento de multa equivalente a 20% "
                        + "(vinte por cento) do valor total do objeto do contrato, operando a rescisão "
                        + "automática do presente Contrato com vencimento antecipado das demais parcelas, "
                        + "bem como as perdas e danos, se couber.</p>",
                null));

        clauses.add(newClause(contractId, 9, "CLÁUSULA NONA - DO EXERCÍCIO DOS DIREITOS",
                "<p>9.1. Aplicam-se ao presente Contrato as disposições do Código Civil e do Código de "
                        + "Defesa do Consumidor naquilo em que lhe forem compatíveis.</p>"
                        + "<p>9.2. Caso seja necessário qualquer outro tipo de serviço técnico em eletricidade, "
                        + "além do objeto descrito no item 1, o mesmo deverá ser discutido antes da "
                        + "execução, cabendo aditivo a este Contrato.</p>",
                null));

        clauses.add(newClause(contractId, 10, "CLÁUSULA DECIMA - DO TÍTULO EXTRA JUDICIAL",
                "<p>10.1. O presente contrato constitui título executivo extrajudicial, nos termos do "
                        + "artigo 585, II do Código de processo Civil.</p>",
                null));

        clauses.add(newClause(contractId, 11, "CLÁUSULA DECIMA PRIMEIRA - DISPOSIÇÕES GERAIS",
                "<p>11.1. A CONTRATADA assume a responsabilidade técnica dos serviços a serem executados, "
                        + "declarando, neste ato, que conhece os equipamentos e o local da prestação de "
                        + "serviços – previamente visitado em vistoria técnica realizada pelo Engenheiro "
                        + "responsável.</p>"
                        + "<p>11.2. A CONTRATADA se compromete a proteger e preservar o meio ambiente, bem "
                        + "como a prevenir contra as práticas danosas ao ecossistema, executando seus "
                        + "serviços em observância dos atos legais normativos e administrativos relativos "
                        + "à área de meio ambiente e as correlatas emanadas das esferas do Governo Federal, "
                        + "Estadual.</p>"
                        + "<p>11.3. As partes de comum acordo elegem o Foro da Comarca de Sumaré/SP para dirimir "
                        + "qualquer lide oriunda do presente Contrato, com renúncia expressa de qualquer "
                        + "outro por mais privilegiado que seja.</p>",
                null));

        return clauses;
    }

    /** Cria uma cláusula com os campos básicos (sem ID, sem auditoria). */
    private static ContractClause newClause(Long contractId, int number, String title,
                                             String content, Long serviceTemplateId) {
        ContractClause clause = new ContractClause();
        clause.setContractId(contractId);
        clause.setClauseNumber(number);
        clause.setTitle(title);
        clause.setContent(content);
        clause.setServiceTemplateId(serviceTemplateId);
        return clause;
    }

    private record ClientResolved(String name, String code) {
        static final ClientResolved EMPTY = new ClientResolved(null, null);
    }
}