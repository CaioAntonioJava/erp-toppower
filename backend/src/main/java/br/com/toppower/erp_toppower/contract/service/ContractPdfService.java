package br.com.toppower.erp_toppower.contract.service;

import br.com.toppower.erp_toppower.common.util.SoftBreak;
import br.com.toppower.erp_toppower.contract.dto.ContractResponse;
import br.com.toppower.erp_toppower.sales.pdf.PdfModelBuilder;
import br.com.toppower.erp_toppower.sales.pdf.SalesPdfService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Orquestra a geração do PDF de um contrato.
 *
 * <p>Mesma estratégia dos demais PDFs do projeto
 * ({@code QuotationPdfService}, {@code TechnicalProposalPdfService},
 * {@code SalesOrderPdfService}): reusa o
 * {@link ContractService#getById} (não duplica lógica de montagem do
 * response) e apenas hidrata os dados complementares que o template
 * Thymeleaf precisa — neste caso, apenas o nome do cliente com quebra
 * "macia" (sufixo societário, separadores semânticos) pré-computado no
 * Java porque o SpEL restrito do Thymeleaf não permite
 * {@code T(SomeClass).method(...)} no template.</p>
 */
@Service
public class ContractPdfService {

    private final ContractService contractService;
    private final SalesPdfService salesPdfService;
    private final PdfModelBuilder pdfModelBuilder;

    public ContractPdfService(ContractService contractService,
                              SalesPdfService salesPdfService,
                              PdfModelBuilder pdfModelBuilder) {
        this.contractService = contractService;
        this.salesPdfService = salesPdfService;
        this.pdfModelBuilder = pdfModelBuilder;
    }

    /**
     * Renderiza o PDF (A4) do contrato.
     *
     * @param id UUID do contrato
     * @return bytes do PDF gerado
     */
    public byte[] renderPdf(UUID id) {
        ContractResponse contract = contractService.getById(id);

        Map<String, Object> model = pdfModelBuilder.buildBaseModel();
        model.put("contract", contract);

        // Nome com quebra "macia" — apenas o nome/razão social do cliente,
        // sem o código. Pré-computado em Java porque o template Thymeleaf
        // não consegue chamar métodos estáticos arbitrários.
        model.put("customerNameHtml", softBroken(contract.clientName()));

        return salesPdfService.render("pdf/contract", model);
    }

    /**
     * Aplica {@link SoftBreak#name(String)} ao valor, retornando
     * {@code null} quando nulo/vazio. O template decide se renderiza o
     * bloco (label + valor) só quando o conteúdo estiver presente.
     */
    private static String softBroken(String value) {
        return (value == null || value.isBlank()) ? null : SoftBreak.name(value);
    }

    /**
     * Helper estático para uso em outros pontos do módulo, se necessário
     * (ex.: um futuro endpoint que retorne apenas o HTML).
     */
    public static Map<String, Object> emptyModel() {
        return new HashMap<>();
    }
}