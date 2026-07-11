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

        // Nome com quebra "macia" — código + nome exibidos juntos no
        // cabeçalho do cliente. Pré-computado em Java porque o template
        // Thymeleaf não consegue chamar métodos estáticos arbitrários.
        String customerName = contract.clientName();
        if (contract.clientCode() != null && customerName != null) {
            customerName = contract.clientCode() + " — " + customerName;
        }
        model.put("customerNameHtml", softBroken(customerName));

        return salesPdfService.render("pdf/contract", model);
    }

    /**
     * Aplica {@link SoftBreak#name(String)} ao valor, retornando
     * {@code "—"} quando nulo/vazio — para que o template use
     * {@code th:utext="${chave}"} sem precisar de guarda.
     */
    private static String softBroken(String value) {
        return (value == null || value.isBlank()) ? "—" : SoftBreak.name(value);
    }

    /**
     * Helper estático para uso em outros pontos do módulo, se necessário
     * (ex.: um futuro endpoint que retorne apenas o HTML).
     */
    public static Map<String, Object> emptyModel() {
        return new HashMap<>();
    }
}