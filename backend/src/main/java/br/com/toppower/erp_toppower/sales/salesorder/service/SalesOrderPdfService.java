package br.com.toppower.erp_toppower.sales.salesorder.service;

import br.com.toppower.erp_toppower.product.dto.ProductResponse;
import br.com.toppower.erp_toppower.product.service.ProductService;
import br.com.toppower.erp_toppower.sales.pdf.PdfModelBuilder;
import br.com.toppower.erp_toppower.sales.pdf.SalesPdfService;
import br.com.toppower.erp_toppower.sales.salesorder.dto.SalesOrderResponse;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Orquestra a geração do PDF de um pedido de venda.
 *
 * <p>Mesma estratégia dos demais serviços de PDF do módulo: reusa o
 * {@link SalesOrderService#getById} e apenas hidrata nomes/códigos de
 * produtos para o template Thymeleaf.</p>
 */
@Service
public class SalesOrderPdfService {

    private final SalesOrderService salesOrderService;
    private final ProductService productService;
    private final SalesPdfService salesPdfService;
    private final PdfModelBuilder pdfModelBuilder;

    public SalesOrderPdfService(SalesOrderService salesOrderService,
                                ProductService productService,
                                SalesPdfService salesPdfService,
                                PdfModelBuilder pdfModelBuilder) {
        this.salesOrderService = salesOrderService;
        this.productService = productService;
        this.salesPdfService = salesPdfService;
        this.pdfModelBuilder = pdfModelBuilder;
    }

    /**
     * Renderiza o PDF (A4) do pedido de venda.
     *
     * @param id UUID do pedido
     * @return bytes do PDF gerado
     */
    public byte[] renderPdf(UUID id) {
        SalesOrderResponse order = salesOrderService.getById(id);

        Map<String, Object> model = pdfModelBuilder.buildBaseModel();
        model.put("order", order);
        model.put("productNames", resolveProductField(order, ProductResponse::name));
        model.put("productCodes", resolveProductField(order, p -> p.code() != null ? p.code() : "—"));

        return salesPdfService.render("pdf/sales-order", model);
    }

    private Map<UUID, String> resolveProductField(SalesOrderResponse order,
                                                   Function<ProductResponse, String> extractor) {
        Map<UUID, String> result = new HashMap<>();
        if (order.items() == null) return result;
        for (var item : order.items()) {
            UUID productUuid = item.productUuid();
            if (productUuid == null || result.containsKey(productUuid)) continue;
            try {
                ProductResponse p = productService.getById(productUuid);
                result.put(productUuid, extractor.apply(p));
            } catch (RuntimeException ex) {
                // produto removido ou inacessível — segue sem nome
            }
        }
        return result;
    }
}