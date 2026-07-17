package br.com.toppower.erp_toppower.contract.mapper;

import br.com.toppower.erp_toppower.contract.dto.ContractClauseRequest;
import br.com.toppower.erp_toppower.contract.dto.ContractClauseResponse;
import br.com.toppower.erp_toppower.contract.dto.ContractCreateRequest;
import br.com.toppower.erp_toppower.contract.dto.ContractResponse;
import br.com.toppower.erp_toppower.contract.dto.ContractUpdateRequest;
import br.com.toppower.erp_toppower.contract.entity.Contract;
import br.com.toppower.erp_toppower.contract.entity.ContractClause;

import java.util.List;

/**
 * Mapper estático (sem MapStruct) para a entidade {@link Contract}.
 *
 * <p>Convenções do projeto:</p>
 * <ul>
 *   <li>O código comercial ({@code prefix}, {@code sequence}, {@code year})
 *   NÃO é setado aqui — é gerado no service a partir da Organization ativa.</li>
 *   <li>O cliente ({@code customerId}/{@code companyId}) é repassado como
 *   está; a validação de invariante (exatamente um) é feita no service.</li>
 *   <li>O {@code title} e a {@code description} podem ser pré-preenchidos
 *   pelo service (defaults do backend / template da Organization) quando
 *   o request não os trouxer.</li>
 * </ul>
 */
public final class ContractMapper {

    private ContractMapper() {
    }

    /**
     * Cria uma nova entidade a partir do request de criação.
     * <ul>
     *   <li>O código comercial NÃO é setado aqui — é gerado no service.</li>
     *   <li>Os IDs de cliente são repassados diretamente.</li>
     *   <li>O {@code title}/{@code description} são repassados como estão;
     *   o service trata os defaults quando o request os traz nulos.</li>
     *   <li>O {@code status} fica nulo; o {@code @PrePersist} da entidade
     *   aplica o default {@code ATIVO}.</li>
     * </ul>
     */
    public static Contract toEntity(ContractCreateRequest request) {
        Contract contract = new Contract();
        contract.setCustomerId(request.customerId());
        contract.setCompanyId(request.companyId());
        contract.setTitle(request.title());
        contract.setDescription(request.description());
        if (request.validityDate() != null) {
            contract.setValidityDate(request.validityDate());
        }
        return contract;
    }

    /**
     * Constrói a resposta completa, incluindo os dados do cliente resolvido
     * (nome, código, tipo) que são passados pelo service, e as cláusulas
     * já carregadas pelo service.
     */
    public static ContractResponse toResponse(Contract contract,
                                               String clientName,
                                               String clientCode,
                                               List<ContractClause> clauses) {
        String clientType = (contract.getCustomerId() != null) ? "CUSTOMER" : "COMPANY";
        List<ContractClauseResponse> clauseResponses = (clauses == null)
                ? List.of()
                : clauses.stream().map(ContractMapper::toClauseResponse).toList();
        return new ContractResponse(
                contract.getId(),
                contract.getPrefix(),
                contract.getSequence(),
                contract.getYear(),
                contract.formattedCode(),
                contract.getCustomerId(),
                contract.getCompanyId(),
                clientType,
                clientName,
                clientCode,
                contract.getTitle(),
                contract.getDescription(),
                contract.getStatus(),
                contract.getValidityDate(),
                contract.getCreatedAt(),
                contract.getUpdatedAt(),
                contract.getCreatedBy(),
                contract.getUpdatedBy(),
                clauseResponses
        );
    }

    // ---------------------------------------------------------------------
    // Cláusulas
    // ---------------------------------------------------------------------

    /**
     * Cria uma entidade de cláusula a partir do request e do ID do contrato pai.
     * O {@code contractId} é injetado pelo service após salvar o contrato pai.
     */
    public static ContractClause toClauseEntity(ContractClauseRequest request, Long contractId) {
        ContractClause clause = new ContractClause();
        clause.setContractId(contractId);
        clause.setClauseNumber(request.clauseNumber());
        clause.setTitle(request.title());
        clause.setContent(request.content());
        clause.setServiceTemplateId(request.serviceTemplateId());
        return clause;
    }

    /**
     * Constrói a resposta de uma cláusula.
     */
    public static ContractClauseResponse toClauseResponse(ContractClause clause) {
        return new ContractClauseResponse(
                clause.getId(),
                clause.getClauseNumber(),
                clause.getTitle(),
                clause.getContent(),
                clause.getServiceTemplateId(),
                clause.getCreatedAt(),
                clause.getUpdatedAt()
        );
    }

    /**
     * Aplica uma atualização parcial (PATCH) na entidade carregada.
     * Apenas campos não nulos do request sobrescrevem o estado atual.
     * <ul>
     *   <li>O código comercial NÃO é alterável — é imutável após a criação.</li>
     *   <li>O cliente pode ser alterado: se {@code customerId} for enviado
     *   (inclusive null), ele é aplicado; o mesmo para {@code companyId}.</li>
     *   <li>{@code title} e {@code description} são livremente editáveis.</li>
     *   <li>{@code status} pode ser alterado via PATCH (alternativa ao
     *   endpoint dedicado de ativar/inativar).</li>
     * </ul>
     */
    public static void applyUpdate(Contract contract, ContractUpdateRequest request) {
        if (request.customerId() != null) {
            contract.setCustomerId(request.customerId());
        }
        if (request.companyId() != null) {
            contract.setCompanyId(request.companyId());
        }
        if (request.title() != null) {
            contract.setTitle(request.title());
        }
        if (request.description() != null) {
            contract.setDescription(request.description());
        }
        if (request.status() != null) {
            contract.setStatus(request.status());
        }
        if (request.validityDate() != null) {
            contract.setValidityDate(request.validityDate());
        }
    }
}