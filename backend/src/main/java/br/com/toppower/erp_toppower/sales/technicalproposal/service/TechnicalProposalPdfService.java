package br.com.toppower.erp_toppower.sales.technicalproposal.service;

import br.com.toppower.erp_toppower.common.util.SoftBreak;
import br.com.toppower.erp_toppower.product.dto.ProductResponse;
import br.com.toppower.erp_toppower.product.service.ProductService;
import br.com.toppower.erp_toppower.sales.pdf.PdfModelBuilder;
import br.com.toppower.erp_toppower.sales.pdf.SalesPdfService;
import br.com.toppower.erp_toppower.sales.technicalproposal.dto.TechnicalProposalResponse;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Orquestra a geração do PDF de uma proposta técnica.
 *
 * <p>Mesma estratégia do {@code QuotationPdfService}: reusa o
 * {@link TechnicalProposalService#getById} (não duplica lógica de
 * montagem do response) e apenas hidrata os dados complementares
 * (nomes/códigos de produtos) que o template Thymeleaf precisa.</p>
 */
@Service
public class TechnicalProposalPdfService {

    private final TechnicalProposalService technicalProposalService;
    private final ProductService productService;
    private final SalesPdfService salesPdfService;
    private final PdfModelBuilder pdfModelBuilder;

    public TechnicalProposalPdfService(TechnicalProposalService technicalProposalService,
                                       ProductService productService,
                                       SalesPdfService salesPdfService,
                                       PdfModelBuilder pdfModelBuilder) {
        this.technicalProposalService = technicalProposalService;
        this.productService = productService;
        this.salesPdfService = salesPdfService;
        this.pdfModelBuilder = pdfModelBuilder;
    }

    /**
     * Renderiza o PDF (A4) da proposta técnica.
     *
     * @param id ID da proposta
     * @return bytes do PDF gerado
     */
    public byte[] renderPdf(Long id) {
        TechnicalProposalResponse proposal = technicalProposalService.getById(id);

        Map<String, Object> model = pdfModelBuilder.buildBaseModel();
        model.put("proposal", proposal);
        model.put("productNames", resolveProductField(proposal, ProductResponse::name));
        model.put("productCodes", resolveProductField(proposal, p -> p.code() != null ? p.code() : "—"));
        // Nome com quebra "macia" (sufixo societário, separadores
        // semânticos) — pré-computado no Java porque o SpEL restrito
        // do Thymeleaf não permite T(SomeClass).method(...) no template.
        // Mantido simétrico com QuotationPdfService e SalesOrderPdfService
        // mesmo que o template atual ainda não o utilize.
        String clientName = proposal.clientName();
        if (proposal.clientCode() != null && clientName != null) {
            clientName = proposal.clientCode() + " — " + clientName;
        }
        model.put("clientNameHtml", softBroken(clientName));

        return salesPdfService.render("pdf/technical-proposal", model);
    }

    private Map<Long, String> resolveProductField(TechnicalProposalResponse proposal,
                                                   Function<ProductResponse, String> extractor) {
        Map<Long, String> result = new HashMap<>();
        if (proposal.productItems() == null) return result;
        for (var item : proposal.productItems()) {
            Long productId = item.productId();
            if (productId == null || result.containsKey(productId)) continue;
            try {
                ProductResponse p = productService.getById(productId);
                result.put(productId, extractor.apply(p));
            } catch (RuntimeException ex) {
                // produto removido ou inacessível — segue sem nome
            }
        }
        return result;
    }

    /**
     * Aplica {@link SoftBreak#name(String)} ao valor, retornando
     * {@code null} quando nulo/vazio. O template decide se renderiza o
     * bloco (label + valor) só quando o conteúdo estiver presente.
     */
    private static String softBroken(String value) {
        return (value == null || value.isBlank()) ? null : SoftBreak.name(value);
    }
}