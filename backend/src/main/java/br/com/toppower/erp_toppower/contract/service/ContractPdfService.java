package br.com.toppower.erp_toppower.contract.service;

import br.com.toppower.erp_toppower.common.util.SoftBreak;
import br.com.toppower.erp_toppower.contract.dto.ContractResponse;
import br.com.toppower.erp_toppower.sales.pdf.PdfModelBuilder;
import br.com.toppower.erp_toppower.sales.pdf.SalesPdfService;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Orquestra a geração do PDF de um contrato de prestação de serviços.
 *
 * <p>Reusa o {@link ContractService#getById} para carregar o contrato
 * (com cliente resolvido e cláusulas) e hidrata os dados complementares
 * que o template Thymeleaf precisa (nome do cliente com quebra macia).</p>
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
     * @param id ID do contrato
     * @return bytes do PDF gerado
     */
    public byte[] renderPdf(Long id) {
        ContractResponse contract = contractService.getById(id);

        Map<String, Object> model = pdfModelBuilder.buildBaseModel();
        model.put("contract", contract);
        model.put("clientNameHtml", softBroken(contract.clientName()));

        return salesPdfService.render("pdf/contract", model);
    }

    /**
     * Aplica {@link SoftBreak#name(String)} ao valor, retornando
     * {@code null} quando nulo/vazio.
     */
    private static String softBroken(String value) {
        return (value == null || value.isBlank()) ? null : SoftBreak.name(value);
    }
}