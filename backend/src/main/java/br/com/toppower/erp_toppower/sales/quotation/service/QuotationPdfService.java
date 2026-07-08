package br.com.toppower.erp_toppower.sales.quotation.service;

import br.com.toppower.erp_toppower.product.dto.ProductResponse;
import br.com.toppower.erp_toppower.product.service.ProductService;
import br.com.toppower.erp_toppower.sales.pdf.PdfModelBuilder;
import br.com.toppower.erp_toppower.sales.pdf.SalesPdfService;
import br.com.toppower.erp_toppower.sales.quotation.dto.QuotationResponse;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Orquestra a geração do PDF de uma proposta comercial.
 *
 * <p>Centraliza a hidratação de dados complementares (nomes e códigos de
 * produtos referenciados nos itens) que o template Thymeleaf precisa,
 * mas que não fazem parte da resposta da API ({@link QuotationResponse})
 * — apenas os UUIDs dos produtos são devolvidos.</p>
 */
@Service
public class QuotationPdfService {

    private final QuotationService quotationService;
    private final ProductService productService;
    private final SalesPdfService salesPdfService;
    private final PdfModelBuilder pdfModelBuilder;

    public QuotationPdfService(QuotationService quotationService,
                               ProductService productService,
                               SalesPdfService salesPdfService,
                               PdfModelBuilder pdfModelBuilder) {
        this.quotationService = quotationService;
        this.productService = productService;
        this.salesPdfService = salesPdfService;
        this.pdfModelBuilder = pdfModelBuilder;
    }

    /**
     * Renderiza o PDF (A4) de uma proposta comercial.
     *
     * @param id UUID da proposta
     * @return bytes do PDF gerado
     */
    public byte[] renderPdf(UUID id) {
        QuotationResponse quotation = quotationService.getById(id);

        Map<String, Object> model = pdfModelBuilder.buildBaseModel();
        model.put("quotation", quotation);
        model.put("productNames", resolveProductNames(quotation));
        model.put("productCodes", resolveProductCodes(quotation));

        return salesPdfService.render("pdf/quotation", model);
    }

    private Map<UUID, String> resolveProductNames(QuotationResponse quotation) {
        return resolveProductField(quotation, ProductResponse::name);
    }

    private Map<UUID, String> resolveProductCodes(QuotationResponse quotation) {
        return resolveProductField(quotation, p -> p.code() != null ? p.code() : "—");
    }

    /**
     * Para cada produto distinto referenciado nos itens da proposta,
     * busca o {@link ProductResponse} e aplica o extrator informado.
     * Falhas individuais (produto deletado, item órfão) não quebram o
     * PDF — apenas omitem aquele item do mapa.
     */
    private Map<UUID, String> resolveProductField(QuotationResponse quotation,
                                                   java.util.function.Function<ProductResponse, String> extractor) {
        Map<UUID, String> result = new HashMap<>();
        if (quotation.items() == null) return result;
        for (var item : quotation.items()) {
            UUID productUuid = item.productUuid();
            if (productUuid == null || result.containsKey(productUuid)) continue;
            try {
                ProductResponse p = productService.getById(productUuid);
                result.put(productUuid, extractor.apply(p));
            } catch (RuntimeException ex) {
                // Produto removido / inacessível — segue sem nome.
            }
        }
        return result;
    }
}