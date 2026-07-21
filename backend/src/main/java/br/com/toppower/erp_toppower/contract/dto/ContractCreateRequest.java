package br.com.toppower.erp_toppower.contract.dto;

import br.com.toppower.erp_toppower.sales.quotation.enums.PaymentCondition;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Dados para cadastro de um novo contrato.
 *
 * <p>O código comercial ({@code <prefix>-<seq>-<year>}, ex.: {@code CL-001-2026})
 * é gerado automaticamente pelo servidor a partir do prefixo configurado na
 * Organization ativa ({@code contract_prefix}) e da sequência independente
 * por Organization/ano. O {@code title} é pré-preenchido pelo backend como
 * {@code "CONTRATO DE PRESTAÇÃO DE SERVIÇOS: <código>"} quando não enviado
 * pelo cliente; a {@code description} pode ser pré-preenchida com o template
 * padrão da Organization quando disponível.</p>
 *
 * <p>O cliente é referenciado por exatamente <b>um</b> dos campos
 * {@code customerId} (pessoa física) ou {@code companyId} (pessoa jurídica).
 * A validação de que ao menos um foi informado (e não ambos) é feita pelo
 * service.</p>
 *
 * <p>Os campos são opcionais exceto {@code price} (preço do contrato,
 * obrigatório): o contrato pode ser criado apenas com os defaults do
 * backend e ter seu conteúdo editado depois.</p>
 */
@Schema(name = "ContractCreateRequest",
        description = "Dados para cadastro de um novo contrato. O código comercial é gerado pelo servidor.")
public record ContractCreateRequest(

        @Schema(description = "ID do cliente pessoa física (Customer). Deve ser informado "
                + "quando o contratado for PF, com companyId nulo.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Long customerId,

        @Schema(description = "ID da empresa pessoa jurídica (Company). Deve ser informado "
                + "quando o contratado for PJ, com customerId nulo.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Long companyId,

        @Schema(description = "Título do contrato. Quando omitido, o backend preenche com "
                + "\"CONTRATO DE PRESTAÇÃO DE SERVIÇOS: <código>\".",
                example = "CONTRATO DE PRESTAÇÃO DE SERVIÇOS: CL-001-2026",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED, maxLength = 300)
        @Size(max = 300, message = "O título deve ter no máximo {max} caracteres")
        String title,

        @Schema(description = "Descrição detalhada do contrato (texto livre / HTML). "
                + "Quando omitido, o backend pode preencher com o template padrão da Organization ativa.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String description,

        @Schema(description = "Data de vigência do contrato. Quando omitida, o backend usa a data atual.",
                example = "2026-07-17", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        LocalDate validityDate,

        @Schema(description = "Preço do contrato (valor informativo, em reais). Obrigatório. "
                + "Não é exibido no PDF do contrato.",
                example = "230800.00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "O preço do contrato é obrigatório")
        @DecimalMin(value = "0.00", inclusive = true, message = "O preço do contrato não pode ser negativo")
        BigDecimal price,

        @Schema(description = "Condição de pagamento do contrato (opcional). "
                + "Reutiliza o mesmo domínio das propostas comerciais.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        PaymentCondition paymentCondition,

        @Schema(description = "Cláusulas do contrato. Quando omitido, o backend pré-preenche "
                + "as 11 cláusulas padrão (cláusula 1 vazia, 2–11 com textos do template padrão).",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Valid
        List<ContractClauseRequest> clauses
) {
}