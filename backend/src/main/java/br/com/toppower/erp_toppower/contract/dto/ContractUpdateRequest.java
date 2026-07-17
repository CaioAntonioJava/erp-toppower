package br.com.toppower.erp_toppower.contract.dto;

import br.com.toppower.erp_toppower.contract.enums.ContractStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Atualização parcial de um contrato (PATCH). Todos os campos são
 * opcionais: envie apenas os campos que deseja alterar.
 *
 * <p>O código comercial ({@code prefix}, {@code sequence}, {@code year})
 * NÃO pode ser alterado após a criação — é imutável. O cliente pode ser
 * alterado enviando {@code customerId} ou {@code companyId} (nunca ambos).
 * O {@code title} e a {@code description} são livremente editáveis.</p>
 */
@Schema(name = "ContractUpdateRequest", description = "Dados para atualização parcial de um contrato (PATCH).")
public record ContractUpdateRequest(

        @Schema(description = "Novo ID do cliente pessoa física (Customer). Envie null para remover "
                + "a referência PF (substituindo por companyId).",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Long customerId,

        @Schema(description = "Novo ID da empresa pessoa jurídica (Company). Envie null para remover "
                + "a referência PJ (substituindo por customerId).",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Long companyId,

        @Schema(description = "Novo título do contrato.", maxLength = 300)
        @Size(max = 300, message = "O título deve ter no máximo {max} caracteres")
        String title,

        @Schema(description = "Nova descrição detalhada do contrato (texto livre / HTML).")
        String description,

        @Schema(description = "Novo status.",
                allowableValues = {"ATIVO", "INATIVO"})
        ContractStatus status,

        @Schema(description = "Nova data de vigência do contrato.",
                example = "2026-07-17", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        LocalDate validityDate
) {
}