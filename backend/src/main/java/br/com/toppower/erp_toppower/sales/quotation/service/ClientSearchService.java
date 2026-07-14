package br.com.toppower.erp_toppower.sales.quotation.service;

import br.com.toppower.erp_toppower.common.enums.RegistrationStatus;
import br.com.toppower.erp_toppower.company.entity.Company;
import br.com.toppower.erp_toppower.company.repository.CompanyRepository;
import br.com.toppower.erp_toppower.customer.entity.Customer;
import br.com.toppower.erp_toppower.customer.repository.CustomerRepository;
import br.com.toppower.erp_toppower.sales.quotation.dto.ClientSummaryResponse;
import br.com.toppower.erp_toppower.sales.quotation.dto.QuotationResponse;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Serviço responsável por buscar clientes (pessoas físicas e jurídicas)
 * para composição de propostas comerciais.
 *
 * <h2>Regra de busca</h2>
 * <p>Para cada termo de busca, retorna clientes cuja condição seja
 * satisfeita:</p>
 * <pre>
 *   (código contém o termo digitado)
 *   OR (documento — CPF/CNPJ — contém o termo digitado)
 *   OR (TODAS as palavras do termo estão contidas no nome)
 *   AND (cliente/empresa está com status ATIVO)
 * </pre>
 *
 * <p>Isso resolve o problema de "todos os cadastrados aparecem": a
 * lógica antiga usava OR entre as palavras (bastava 1 palavra bater
 * para incluir o registro) e ainda aplicava OR também com o filtro de
 * status, fazendo praticamente qualquer busca retornar tudo.</p>
 *
 * <h2>Exemplos</h2>
 * <ul>
 *   <li>Termo <code>"VARIEDADES COMERCIO SUPRIMENTOS INDUSTRIAIS LTDA"</code>
 *       → só retorna empresas cujas razões sociais/nomes fantasia
 *       contenham <b>todas</b> essas palavras. "PETROLEO BRASILEIRO"
 *       não casa.</li>
 *   <li>Termo <code>"CLI000042"</code> → casa se algum cliente tiver
 *       <code>code LIKE '%CLI000042%'</code>.</li>
 *   <li>Termo <code>"123.456.789-00"</code> (CPF) ou <code>"12345678900"</code>
 *       (somente dígitos) → casa clientes PF cujo CPF contenha o termo.</li>
 *   <li>Termo <code>"12.345.678/0001-90"</code> (CNPJ) ou
 *       <code>"12345678000190"</code> → casa empresas PJ cujo CNPJ contenha
 *       o termo.</li>
 *   <li>Termo <code>"JOAO"</code> (uma palavra) → casa clientes
 *       cujo nome contém "JOAO".</li>
 * </ul>
 */
@Service
public class ClientSearchService {

    private final CustomerRepository customerRepository;
    private final CompanyRepository companyRepository;

    public ClientSearchService(CustomerRepository customerRepository,
                               CompanyRepository companyRepository) {
        this.customerRepository = customerRepository;
        this.companyRepository = companyRepository;
    }

    /** Tamanho mínimo (em caracteres) do termo de busca. */
    public static final int MIN_SEARCH_QUERY_LENGTH = 2;

    /** Limite máximo padrão quando o caller não informa um. */
    public static final int DEFAULT_LIMIT = 20;

    /** Limite máximo aceito para evitar explosão de resultados. */
    public static final int MAX_LIMIT = 100;

    /**
     * Busca clientes (PF e PJ) por match no código (substring) OU por
     * palavras-chave contidas no nome. Apenas clientes e empresas com
     * status {@link RegistrationStatus#ATIVO} são retornados.
     *
     * @param query termo de busca (mínimo 2 caracteres)
     * @param limit limite de resultados (clampado a {@link #MAX_LIMIT})
     * @param type  filtro opcional de tipo — quando informado como
     *              {@link QuotationResponse.ClientType#CUSTOMER} ou
     *              {@link QuotationResponse.ClientType#COMPANY}, a busca
     *              retorna apenas registros daquele tipo. {@code null}
     *              (ou qualquer valor fora do enum) mantém o
     *              comportamento original: PF + PJ juntos.
     * @return lista de {@link ClientSummaryResponse} ordenada por nome
     */
    @Transactional(readOnly = true)
    public List<ClientSummaryResponse> search(String query, Integer limit, QuotationResponse.ClientType type) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String trimmed = query.trim();
        if (trimmed.length() < MIN_SEARCH_QUERY_LENGTH) {
            throw new IllegalArgumentException(
                    "O termo de busca deve ter ao menos " + MIN_SEARCH_QUERY_LENGTH + " caracteres");
        }
        int effectiveLimit = (limit == null || limit <= 0) ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);

        // Quebra em palavras (case-insensitive). Filtra vazias.
        String[] words = trimmed.split("\\s+");
        List<String> wordList = new ArrayList<>(words.length);
        for (String w : words) {
            if (w != null && !w.isBlank()) {
                wordList.add(w);
            }
        }

        // Filtro de tipo. Se `null` ou vazio, mantém o comportamento de
        // retornar PF + PJ. Valores desconhecidos são ignorados (cai no
        // "ambos") para preservar compatibilidade com callers antigos.
        boolean includeCustomers = (type == null) || (type == QuotationResponse.ClientType.CUSTOMER);
        boolean includeCompanies = (type == null) || (type == QuotationResponse.ClientType.COMPANY);

        List<ClientSummaryResponse> results = new ArrayList<>();

        // Pessoas físicas: code contém o termo OU name contém TODAS as palavras
        if (includeCustomers) {
            customerRepository.findAll(buildCustomerSpec(wordList, trimmed))
                    .stream()
                    .map(c -> toClientSummary(
                            QuotationResponse.ClientType.CUSTOMER,
                            c.getId(),
                            c.getCode(),
                            c.getName(),
                            c.getCpf()))
                    .forEach(results::add);
        }

        // Pessoas jurídicas: code contém o termo OU (legalName contém todas
        // as palavras) OU (tradeName contém todas as palavras)
        if (includeCompanies) {
            companyRepository.findAll(buildCompanySpec(wordList, trimmed))
                    .stream()
                    .map(c -> toClientSummary(
                            QuotationResponse.ClientType.COMPANY,
                            c.getId(),
                            c.getCode(),
                            c.getTradeName() != null && !c.getTradeName().isBlank()
                                    ? c.getTradeName()
                                    : c.getLegalName(),
                            c.getCnpj()))
                    .forEach(results::add);
        }

        return results.stream()
                .sorted(Comparator.comparing(ClientSummaryResponse::name, String.CASE_INSENSITIVE_ORDER))
                .limit(effectiveLimit)
                .toList();
    }

    /**
     * Sobrecarga de compatibilidade: sem filtro de tipo. Equivalente a
     * {@code search(query, limit, null)}.
     */
    @Transactional(readOnly = true)
    public List<ClientSummaryResponse> search(String query, Integer limit) {
        return search(query, limit, null);
    }

    /**
     * Specification para Customer:
     * <pre>
     *   (code LIKE %query%) OR (cpf LIKE %query%) OR (name contém TODAS as palavras)
     *   AND status = ATIVO
     * </pre>
     */
    private static Specification<Customer> buildCustomerSpec(List<String> words, String fullQuery) {
        return (root, query, cb) -> {
            // 1) name contém TODAS as palavras
            Predicate allWordsInName = cb.conjunction();
            for (String word : words) {
                allWordsInName = cb.and(allWordsInName,
                        cb.like(cb.lower(root.get("name")),
                                "%" + word.toLowerCase() + "%"));
            }

            // 2) code contém o termo completo (LIKE)
            Predicate codeMatches = cb.like(
                    cb.lower(root.get("code")),
                    "%" + fullQuery.toLowerCase() + "%");

            // 3) cpf contém o termo completo (LIKE) — match formatado ou só dígitos
            Predicate cpfMatches = cb.like(
                    root.get("cpf"),
                    "%" + fullQuery + "%");

            // 4) (code contém) OR (cpf contém) OR (name contém todas as palavras)
            Predicate nameOrCodeOrDoc = cb.or(codeMatches, cpfMatches, allWordsInName);

            // 5) AND status = ATIVO
            Predicate active = cb.equal(root.get("status"), RegistrationStatus.ATIVO);

            return cb.and(nameOrCodeOrDoc, active);
        };
    }

    /**
     * Specification para Company:
     * <pre>
     *   (code LIKE %query%)
     *   OR (cnpj LIKE %query%)
     *   OR (legalName contém TODAS as palavras)
     *   OR (tradeName contém TODAS as palavras)
     *   AND status = ATIVO
     * </pre>
     */
    private static Specification<Company> buildCompanySpec(List<String> words, String fullQuery) {
        return (root, query, cb) -> {
            // legalName contém TODAS as palavras
            Predicate allWordsInLegalName = cb.conjunction();
            for (String word : words) {
                allWordsInLegalName = cb.and(allWordsInLegalName,
                        cb.like(cb.lower(root.get("legalName")),
                                "%" + word.toLowerCase() + "%"));
            }
            // tradeName contém TODAS as palavras
            Predicate allWordsInTradeName = cb.conjunction();
            for (String word : words) {
                allWordsInTradeName = cb.and(allWordsInTradeName,
                        cb.like(cb.lower(root.get("tradeName")),
                                "%" + word.toLowerCase() + "%"));
            }
            // code contém o termo completo
            Predicate codeMatches = cb.like(
                    cb.lower(root.get("code")),
                    "%" + fullQuery.toLowerCase() + "%");
            // cnpj contém o termo completo (LIKE) — match formatado ou só dígitos
            Predicate cnpjMatches = cb.like(
                    root.get("cnpj"),
                    "%" + fullQuery + "%");

            // (code) OR (cnpj) OR (legalName todas) OR (tradeName todas)
            Predicate nameOrCodeOrDoc = cb.or(
                    codeMatches, cnpjMatches, allWordsInLegalName, allWordsInTradeName);

            // AND status = ATIVO
            Predicate active = cb.equal(root.get("status"), RegistrationStatus.ATIVO);

            return cb.and(nameOrCodeOrDoc, active);
        };
    }

    private static ClientSummaryResponse toClientSummary(QuotationResponse.ClientType type,
                                                         Long id,
                                                         String code,
                                                         String name,
                                                         String document) {
        return new ClientSummaryResponse(type, id, code, name, document);
    }
}
