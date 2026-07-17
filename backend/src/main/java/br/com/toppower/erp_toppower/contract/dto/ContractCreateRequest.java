package br.com.toppower.erp_toppower.contract.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

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
 * <p>Todos os campos são opcionais: o contrato pode ser criado apenas com
 * os defaults do backend e ter seu conteúdo editado depois.</p>
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
        LocalDate validityDate
) {
}