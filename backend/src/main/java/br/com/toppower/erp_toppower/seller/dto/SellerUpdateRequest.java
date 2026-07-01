package br.com.toppower.erp_toppower.seller.dto;

import br.com.toppower.erp_toppower.seller.enums.SellerStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Atualização parcial do vendedor (PATCH). Todos os campos são opcionais:
 * envie apenas os campos que deseja alterar.
 */
@Schema(name = "SellerUpdateRequest", description = "Dados para atualização parcial de um vendedor (PATCH).")
public record SellerUpdateRequest(

        @Schema(description = "Novo nome completo.", maxLength = 150)
        @Size(max = 150, message = "Nome deve ter no máximo {max} caracteres")
        String name,

        @Schema(description = "Novo e-mail de contato.", maxLength = 150)
        @Email(message = "E-mail inválido")
        @Size(max = 150, message = "E-mail deve ter no máximo {max} caracteres")
        String email,

        @Schema(description = "Novo telefone de contato.", maxLength = 20)
        @Size(max = 20, message = "Telefone deve ter no máximo {max} caracteres")
        String phone,

        @Schema(description = "Novo CPF.", maxLength = 14)
        @Size(max = 14, message = "CPF deve ter no máximo {max} caracteres")
        @Pattern(regexp = "\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}",
                message = "CPF deve estar no formato 000.000.000-00 ou conter 11 dígitos")
        String cpf,

        @Schema(description = "Novo percentual de comissão (ex: 5.50 = 5,50%). Aceita 0.00. Faixa válida quando informado: 0,00% a 100,00%.",
                minimum = "0.0", maximum = "100.0", nullable = true)
        @DecimalMin(value = "0.00", message = "Comissão deve ser no mínimo 0%")
        @DecimalMax(value = "100.00", message = "Comissão deve ser no máximo 100%")
        @Digits(integer = 3, fraction = 2, message = "Comissão deve ter no máximo 2 casas decimais")
        BigDecimal commissionRate,

        @Schema(description = "Novo status do vendedor.",
                allowableValues = {"ATIVO", "INATIVO"})
        SellerStatus status
) {
}
