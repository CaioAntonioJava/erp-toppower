package br.com.toppower.erp_toppower.seller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(name = "SellerCreateRequest", description = "Dados para cadastro de um novo vendedor.")
public record SellerCreateRequest(

        @Schema(description = "Nome completo do vendedor.", example = "João da Silva",
                requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 150)
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 150, message = "Nome deve ter no máximo {max} caracteres")
        String name,

        @Schema(description = "E-mail único do vendedor.",
                example = "joao@toppower.com.br", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 150)
        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "E-mail inválido")
        @Size(max = 150, message = "E-mail deve ter no máximo {max} caracteres")
        String email,

        @Schema(description = "Telefone de contato.", example = "(11) 98765-4321",
                requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 20)
        @NotBlank(message = "Telefone é obrigatório")
        @Size(max = 20, message = "Telefone deve ter no máximo {max} caracteres")
        String phone,

        @Schema(description = "CPF único (somente dígitos ou formatado).",
                example = "123.456.789-00",
                requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 14)
        @NotBlank(message = "CPF é obrigatório")
        @Size(max = 14, message = "CPF deve ter no máximo {max} caracteres")
        @Pattern(regexp = "\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}",
                message = "CPF deve estar no formato 000.000.000-00 ou conter 11 dígitos")
        String cpf,

        @Schema(description = "Percentual de comissão do vendedor (ex: 5.50 = 5,50%). Opcional: aceita null ou 0.00. Faixa válida quando informado: 0,00% a 100,00%.",
                example = "5.50", requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                minimum = "0.0", maximum = "100.0", nullable = true)
        @DecimalMin(value = "0.00", message = "Comissão deve ser no mínimo 0%")
        @DecimalMax(value = "100.00", message = "Comissão deve ser no máximo 100%")
        @Digits(integer = 3, fraction = 2, message = "Comissão deve ter no máximo 2 casas decimais")
        BigDecimal commissionRate
) {
}
