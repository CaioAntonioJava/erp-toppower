package br.com.toppower.erp_toppower.contractfinances.mapper;

import br.com.toppower.erp_toppower.contractfinances.dto.ContractFinanceEntryCreateRequest;
import br.com.toppower.erp_toppower.contractfinances.dto.ContractFinanceEntryResponse;
import br.com.toppower.erp_toppower.contractfinances.dto.ContractFinanceEntryUpdateRequest;
import br.com.toppower.erp_toppower.contractfinances.dto.ContractFinanceControlResponse;
import br.com.toppower.erp_toppower.contractfinances.entity.ContractFinanceControl;
import br.com.toppower.erp_toppower.contractfinances.entity.ContractFinanceEntry;
import br.com.toppower.erp_toppower.contractfinances.enums.ContractFinanceEntryStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Mapeadores (estáticos) do financeiro de contratos — padrão do projeto
 * (classe final, sem MapStruct).
 */
public final class ContractFinancesMapper {

    private ContractFinancesMapper() {
    }

    /** Cria a entidade de lançamento a partir do request de criação (status default ATIVO). */
    public static ContractFinanceEntry toEntryEntity(ContractFinanceEntryCreateRequest request,
                                                     Long controlId) {
        ContractFinanceEntry e = new ContractFinanceEntry();
        e.setControlId(controlId);
        e.setType(request.type());
        e.setEntryDate(request.entryDate());
        e.setAmount(request.amount().setScale(2, RoundingMode.HALF_UP));
        e.setDescription(request.description());
        e.setStatus((request.status() != null)
                ? request.status() : ContractFinanceEntryStatus.ATIVO);
        return e;
    }

    /** Monta a resposta do lançamento com os dados do controle resolvidos. */
    public static ContractFinanceEntryResponse toResponse(ContractFinanceEntry e,
                                                          ContractFinanceControl control) {
        return new ContractFinanceEntryResponse(
                e.getId(),
                e.getControlId(),
                (control != null) ? control.getContractId() : null,
                (control != null) ? control.getContractCode() : null,
                (control != null) ? control.getClientType() : null,
                (control != null) ? control.getClientCode() : null,
                (control != null) ? control.getClientName() : null,
                e.getType(),
                e.getEntryDate(),
                e.getAmount(),
                e.getDescription(),
                e.getStatus(),
                e.getCreatedAt(),
                e.getUpdatedAt(),
                e.getCreatedBy(),
                e.getUpdatedBy());
    }

    /** Aplica o PATCH parcial ao lançamento (nulos preservam o valor atual). */
    public static void applyEntryUpdate(ContractFinanceEntry e,
                                        ContractFinanceEntryUpdateRequest request) {
        if (request.type() != null) {
            e.setType(request.type());
        }
        if (request.entryDate() != null) {
            e.setEntryDate(request.entryDate());
        }
        if (request.amount() != null) {
            e.setAmount(request.amount().setScale(2, RoundingMode.HALF_UP));
        }
        if (request.description() != null) {
            e.setDescription(request.description());
        }
    }

    /**
     * Monta a resposta do controle com os totais agregados informados
     * (somas de RECEBIDO e de todas as categorias de gasto, contagem).
     */
    public static ContractFinanceControlResponse toResponse(ContractFinanceControl c,
                                                            BigDecimal totalRecebido,
                                                            BigDecimal totalGasto,
                                                            long entriesCount) {
        BigDecimal received = scaleZero(totalRecebido);
        BigDecimal cost = scaleZero(totalGasto);
        return new ContractFinanceControlResponse(
                c.getId(),
                c.getContractId(),
                c.getContractCode(),
                c.getClientType(),
                c.getClientCode(),
                c.getClientName(),
                c.getContractDate(),
                c.getContractValue(),
                received,
                cost,
                received.subtract(cost).setScale(2, RoundingMode.HALF_UP),
                entriesCount,
                c.getCreatedAt(),
                c.getUpdatedAt(),
                c.getCreatedBy(),
                c.getUpdatedBy());
    }

    private static BigDecimal scaleZero(BigDecimal value) {
        return ((value != null) ? value : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }
}
