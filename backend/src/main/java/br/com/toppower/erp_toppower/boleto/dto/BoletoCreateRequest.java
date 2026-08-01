package br.com.toppower.erp_toppower.boleto.dto;

import br.com.toppower.erp_toppower.common.enums.RegistrationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(name = "BoletoCreateRequest", description = "Dados para cadastro de um novo boleto. "
        + "Suporta parcelamento: informe installmentsCount > 1 e installmentTerms (ex.: \"30/60/90\") "
        + "para gerar N boletos, cada um replicado no contas a pagar.")
public record BoletoCreateRequest(

        @Schema(description = "Descrição do boleto.",
                example = "Pagamento fornecedor XYZ", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 200)
        @NotBlank(message = "Descrição do boleto é obrigatória")
        @Size(max = 200, message = "Descrição do boleto deve ter no máximo {max} caracteres")
        String description,

        @Schema(description = "Beneficiário do boleto (quem recebe o pagamento). Opcional.",
                example = "EMPRESA XPTO LTDA", requiredMode = Schema.RequiredMode.NOT_REQUIRED, maxLength = 200)
        @Size(max = 200, message = "Beneficiário deve ter no máximo {max} caracteres")
        String payee,

        @Schema(description = "Valor total do boleto (ou valor total parcelado).",
                example = "1500.00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Valor é obrigatório")
        @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
        BigDecimal value,

        @Schema(description = "Data de vencimento do boleto. Ignorada quando installmentsCount > 1 "
                + "(os vencimentos são derivados de installmentTerms).",
                example = "2026-08-17", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Data de vencimento é obrigatória")
        @FutureOrPresent(message = "Data de vencimento deve ser hoje ou uma data futura")
        LocalDate dueDate,

        @Schema(description = "Status inicial. Se omitido, assume ATIVO.",
                example = "ATIVO", allowableValues = {"ATIVO", "INATIVO"},
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        RegistrationStatus status,

        @Schema(description = "ID do fornecedor (supplier) vinculado. Se omitido, o sistema "
                + "vincula automaticamente o fornecedor padrão (\"Boleto Avulso\") para que a "
                + "conta a pagar tenha um devedor. O cadastro do boleto sempre dispara a geração "
                + "automática de uma conta a pagar.",
                example = "12", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Long supplierId,

        @Schema(description = "Nº de Contrato/Obra vinculado ao boleto (campo livre, opcional).",
                example = "CT-001-2026", requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                maxLength = 60)
        @Size(max = 60, message = "Nº Contrato/Obra deve ter no máximo {max} caracteres")
        String contractWorkNumber,

        @Schema(description = "Data de cadastro do boleto (informável pelo usuário). Default: data atual.",
                example = "2026-08-01", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        LocalDate registrationDate,

        @Schema(description = "Quantidade de parcelas a gerar. Default 1 (boleto avulso). "
                + "Quando > 1, o sistema divide o valor total em N boletos e gera os vencimentos "
                + "a partir de installmentTerms.",
                example = "3", requiredMode = Schema.RequiredMode.NOT_REQUIRED, minimum = "1")
        @Min(value = 1, message = "Quantidade de parcelas deve ser no mínimo 1")
        Integer installmentsCount,

        @Schema(description = "Prazos das parcelas em dias, separados por barra, usados quando "
                + "installmentsCount > 1. Ex.: \"30/60/90\" gera 3 parcelas vencendo em "
                + "registrationDate+30, +60 e +90. Deve conter tantos prazos quantas parcelas.",
                example = "30/60/90", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String installmentTerms
) {
}