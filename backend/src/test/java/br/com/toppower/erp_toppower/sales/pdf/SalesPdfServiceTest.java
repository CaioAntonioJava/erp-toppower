package br.com.toppower.erp_toppower.sales.pdf;

import br.com.toppower.erp_toppower.sales.quotation.enums.FreightType;
import br.com.toppower.erp_toppower.sales.quotation.enums.PaymentCondition;
import br.com.toppower.erp_toppower.sales.quotation.enums.QuotationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Teste de fumaça do {@link SalesPdfService} — renderiza cada um dos
 * 3 templates (quotation, technical-proposal, sales-order) com um
 * modelo mínimo e verifica que o PDF resultante começa com a assinatura
 * {@code %PDF-}.
 *
 * <p>Útil para detectar regressões nos templates (referências a campos
 * inexistentes, chamadas NPE em enums nulos, fragmentos não resolvidos,
 * etc.).</p>
 *
 * <p><b>Configuração do TemplateEngine:</b> usamos
 * {@link SpringTemplateEngine} para garantir que o Thymeleaf 3.1+ use
 * SpEL (não OGNL) na avaliação de expressões — espelhando o que o
 * Spring Boot auto-configura em produção. Instanciar {@code new
 * TemplateEngine()} puro faz o Thymeleaf cair no padrão legado (OGNL),
 * que não está mais no classpath a partir do Thymeleaf 3.1.</p>
 */
class SalesPdfServiceTest {

    private SalesPdfService service;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);

        TemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        service = new SalesPdfService(engine);
    }

    @Test
    void renderQuotationPdf_producesValidPdf() {
        Map<String, Object> model = baseModel();
        // Usamos um record-like via Map com todas as chaves esperadas pelo
        // template (incluindo as nullable) — SpEL precisa que as propriedades
        // existam, mesmo com valor null. Em produção, o QuotationResponse é
        // um record Java e funciona nativamente.
        QuotationLike quotation = new QuotationLike(
                1500L,
                LocalDate.of(2026, 7, 8),
                "Cliente Teste LTDA",
                "EMP000001",
                "Sr. João",
                "Vendedor Teste",
                QuotationStatus.ATIVA,
                15,
                PaymentCondition.PIX,
                FreightType.CIF,
                new BigDecimal("45.90"),
                null,           // discountType
                null,           // discount
                "Transportadora X",
                new BigDecimal("10.00"),
                new BigDecimal("1000.00"),
                new BigDecimal("1010.45"),
                new BigDecimal("50.00"),
                null,           // notes
                List.of()       // items
        );
        model.put("quotation", quotation);
        // Nomes com soft-break pré-computados (como QuotationPdfService faz
        // em produção). Sem isso, o template não resolve as expressões
        // th:utext="${clientNameHtml}".
        model.put("clientNameHtml",
                br.com.toppower.erp_toppower.common.util.SoftBreak.name("Cliente Teste LTDA"));
        model.put("attentionHtml",
                br.com.toppower.erp_toppower.common.util.SoftBreak.name("Sr. João"));

        assertProducesValidPdf("pdf/quotation", model, "cotação");
    }

    /**
     * Espelha o shape de {@code QuotationResponse} com getters estilo JavaBean.
     * Existe porque o SpEL do Thymeleaf não consegue ler campos ausentes de
     * um {@link HashMap} — o erro é {@code EL1008E}, mesmo quando a chave
     * for null. Em produção o template recebe o record Java real, então
     * essa fragilidade só afeta o teste.
     */
    @SuppressWarnings("unused")
    private static class QuotationLike {
        private final Long number;
        private final LocalDate issueDate;
        private final String clientName;
        private final String clientCode;
        private final String attention;
        private final String sellerName;
        private final QuotationStatus status;
        private final Integer validityDays;
        private final PaymentCondition paymentCondition;
        private final FreightType freightType;
        private final BigDecimal freightValue;
        private final Object discountType; // DiscountType? no template só importa name()
        private final BigDecimal discount;
        private final String carrierName;
        private final BigDecimal profitMargin;
        private final BigDecimal subtotal;
        private final BigDecimal total;
        private final BigDecimal globalDiscountValue;
        private final String notes;
        private final List<?> items;

        QuotationLike(Long number, LocalDate issueDate, String clientName, String clientCode,
                      String attention, String sellerName, QuotationStatus status, Integer validityDays,
                      PaymentCondition paymentCondition, FreightType freightType, BigDecimal freightValue,
                      Object discountType, BigDecimal discount, String carrierName,
                      BigDecimal profitMargin, BigDecimal subtotal, BigDecimal total,
                      BigDecimal globalDiscountValue, String notes, List<?> items) {
            this.number = number; this.issueDate = issueDate; this.clientName = clientName;
            this.clientCode = clientCode; this.attention = attention; this.sellerName = sellerName;
            this.status = status; this.validityDays = validityDays; this.paymentCondition = paymentCondition;
            this.freightType = freightType; this.freightValue = freightValue;
            this.discountType = discountType; this.discount = discount;
            this.carrierName = carrierName; this.profitMargin = profitMargin;
            this.subtotal = subtotal; this.total = total;
            this.globalDiscountValue = globalDiscountValue;
            this.notes = notes; this.items = items;
        }

        public Long getNumber() { return number; }
        public LocalDate getIssueDate() { return issueDate; }
        public String getClientName() { return clientName; }
        public String getClientCode() { return clientCode; }
        public String getAttention() { return attention; }
        public String getSellerName() { return sellerName; }
        public QuotationStatus getStatus() { return status; }
        public Integer getValidityDays() { return validityDays; }
        public PaymentCondition getPaymentCondition() { return paymentCondition; }
        public FreightType getFreightType() { return freightType; }
        public BigDecimal getFreightValue() { return freightValue; }
        public Object getDiscountType() { return discountType; }
        public BigDecimal getDiscount() { return discount; }
        public String getCarrierName() { return carrierName; }
        public BigDecimal getProfitMargin() { return profitMargin; }
        public BigDecimal getSubtotal() { return subtotal; }
        public BigDecimal getTotal() { return total; }
        public BigDecimal getGlobalDiscountValue() { return globalDiscountValue; }
        public String getNotes() { return notes; }
        public List<?> getItems() { return items; }
    }

    /**
     * Espelha o shape de {@code SalesOrderResponse} com getters estilo
     * JavaBean. Existe pelo mesmo motivo da {@link QuotationLike}: o
     * SpEL do Thymeleaf precisa que as propriedades existam para
     * resolver {@code order.field}. Em produção o template recebe o
     * record real.
     */
    @SuppressWarnings("unused")
    private static class SalesOrderLike {
        private final Long number;
        private final LocalDate orderDate;
        private final String clientName;
        private final String clientCode;
        private final String attention;
        private final String sellerName;
        private final br.com.toppower.erp_toppower.sales.salesorder.enums.SalesOrderStatus status;
        private final Long quotationNumber;
        private final PaymentCondition paymentCondition;
        private final FreightType freightType;
        private final BigDecimal freightValue;
        private final Object discountType;
        private final BigDecimal discount;
        private final String carrierName;
        private final BigDecimal subtotal;
        private final BigDecimal total;
        private final BigDecimal globalDiscountValue;
        private final String notes;
        private final List<?> items;

        SalesOrderLike(Long number, LocalDate orderDate, String clientName, String clientCode,
                       String attention, String sellerName,
                       br.com.toppower.erp_toppower.sales.salesorder.enums.SalesOrderStatus status,
                       Long quotationNumber, PaymentCondition paymentCondition, FreightType freightType,
                       BigDecimal freightValue, Object discountType, BigDecimal discount,
                       String carrierName, BigDecimal subtotal, BigDecimal total,
                       BigDecimal globalDiscountValue, String notes, List<?> items) {
            this.number = number; this.orderDate = orderDate;
            this.clientName = clientName; this.clientCode = clientCode;
            this.attention = attention; this.sellerName = sellerName;
            this.status = status; this.quotationNumber = quotationNumber;
            this.paymentCondition = paymentCondition; this.freightType = freightType;
            this.freightValue = freightValue;
            this.discountType = discountType; this.discount = discount;
            this.carrierName = carrierName;
            this.subtotal = subtotal; this.total = total;
            this.globalDiscountValue = globalDiscountValue;
            this.notes = notes; this.items = items;
        }

        public Long getNumber() { return number; }
        public LocalDate getOrderDate() { return orderDate; }
        public String getClientName() { return clientName; }
        public String getClientCode() { return clientCode; }
        public String getAttention() { return attention; }
        public String getSellerName() { return sellerName; }
        public br.com.toppower.erp_toppower.sales.salesorder.enums.SalesOrderStatus getStatus() { return status; }
        public Long getQuotationNumber() { return quotationNumber; }
        public PaymentCondition getPaymentCondition() { return paymentCondition; }
        public FreightType getFreightType() { return freightType; }
        public BigDecimal getFreightValue() { return freightValue; }
        public Object getDiscountType() { return discountType; }
        public BigDecimal getDiscount() { return discount; }
        public String getCarrierName() { return carrierName; }
        public BigDecimal getSubtotal() { return subtotal; }
        public BigDecimal getTotal() { return total; }
        public BigDecimal getGlobalDiscountValue() { return globalDiscountValue; }
        public String getNotes() { return notes; }
        public List<?> getItems() { return items; }
    }

    /**
     * Espelha o shape de {@code TechnicalProposalResponse} com getters
     * JavaBean. Apenas os getters realmente referenciados pelo template
     * {@code pdf/technical-proposal.html} foram implementados.
     *
     * <p>O template chama {@code proposal.clientType.name()} — o getter
     * {@code getClientType()} devolve um {@link Object} (no teste
     * passamos uma String, suficiente para o SpEL resolver
     * {@code .name()}).</p>
     */
    @SuppressWarnings("unused")
    private static class TechnicalProposalLike {
        private final String code;
        private final br.com.toppower.erp_toppower.sales.technicalproposal.enums.TechnicalProposalStatus status;
        private final String clientName;
        private final String clientCode;
        private final Object clientType; // enum aninhado ClientType (CUSTOMER/COMPANY)
        private final String technicalResponsible;
        private final String email;
        private final String phone;
        private final Object address; // TechnicalProposalAddressResponse (nullable)
        private final List<?> objectives;
        private final String description;
        private final LocalDate startDate;
        private final LocalDate endDate;
        private final LocalDate deliveryDate;
        private final List<?> serviceItems;
        private final List<?> productItems;
        private final Object discountType;
        private final BigDecimal discount;
        private final BigDecimal freightValue;
        private final String deliveryDeadline;
        private final PaymentCondition paymentCondition;
        private final String validity;
        private final FreightType deliveryType;
        private final String carrierName;
        private final BigDecimal servicesSubtotal;
        private final BigDecimal productsSubtotal;
        private final BigDecimal subtotal;
        private final BigDecimal globalDiscountValue;
        private final BigDecimal total;
        private final String notes;

        TechnicalProposalLike(String code,
                              br.com.toppower.erp_toppower.sales.technicalproposal.enums.TechnicalProposalStatus status,
                              String clientName, String clientCode, Object clientType,
                              String technicalResponsible, String email, String phone,
                              Object address, List<?> objectives, String description,
                              LocalDate startDate, LocalDate endDate, LocalDate deliveryDate,
                              List<?> serviceItems, List<?> productItems,
                              Object discountType, BigDecimal discount, BigDecimal freightValue,
                              String deliveryDeadline, PaymentCondition paymentCondition,
                              String validity, FreightType deliveryType, String carrierName,
                              BigDecimal servicesSubtotal, BigDecimal productsSubtotal,
                              BigDecimal subtotal, BigDecimal globalDiscountValue,
                              BigDecimal total, String notes) {
            this.code = code; this.status = status;
            this.clientName = clientName; this.clientCode = clientCode;
            this.clientType = clientType;
            this.technicalResponsible = technicalResponsible; this.email = email; this.phone = phone;
            this.address = address; this.objectives = objectives;
            this.description = description;
            this.startDate = startDate; this.endDate = endDate; this.deliveryDate = deliveryDate;
            this.serviceItems = serviceItems; this.productItems = productItems;
            this.discountType = discountType; this.discount = discount;
            this.freightValue = freightValue;
            this.deliveryDeadline = deliveryDeadline;
            this.paymentCondition = paymentCondition;
            this.validity = validity; this.deliveryType = deliveryType;
            this.carrierName = carrierName;
            this.servicesSubtotal = servicesSubtotal;
            this.productsSubtotal = productsSubtotal;
            this.subtotal = subtotal;
            this.globalDiscountValue = globalDiscountValue;
            this.total = total; this.notes = notes;
        }

        public String getCode() { return code; }
        public br.com.toppower.erp_toppower.sales.technicalproposal.enums.TechnicalProposalStatus getStatus() { return status; }
        public String getClientName() { return clientName; }
        public String getClientCode() { return clientCode; }
        public Object getClientType() { return clientType; }
        public String getTechnicalResponsible() { return technicalResponsible; }
        public String getEmail() { return email; }
        public String getPhone() { return phone; }
        public Object getAddress() { return address; }
        public List<?> getObjectives() { return objectives; }
        public String getDescription() { return description; }
        public LocalDate getStartDate() { return startDate; }
        public LocalDate getEndDate() { return endDate; }
        public LocalDate getDeliveryDate() { return deliveryDate; }
        public List<?> getServiceItems() { return serviceItems; }
        public List<?> getProductItems() { return productItems; }
        public Object getDiscountType() { return discountType; }
        public BigDecimal getDiscount() { return discount; }
        public BigDecimal getFreightValue() { return freightValue; }
        public String getDeliveryDeadline() { return deliveryDeadline; }
        public PaymentCondition getPaymentCondition() { return paymentCondition; }
        public String getValidity() { return validity; }
        public FreightType getDeliveryType() { return deliveryType; }
        public String getCarrierName() { return carrierName; }
        public BigDecimal getServicesSubtotal() { return servicesSubtotal; }
        public BigDecimal getProductsSubtotal() { return productsSubtotal; }
        public BigDecimal getSubtotal() { return subtotal; }
        public BigDecimal getGlobalDiscountValue() { return globalDiscountValue; }
        public BigDecimal getTotal() { return total; }
        public String getNotes() { return notes; }
    }

    @Test
    void renderSalesOrderPdf_producesValidPdf() {
        Map<String, Object> model = baseModel();
        SalesOrderLike order = new SalesOrderLike(
                1000L,
                LocalDate.of(2026, 7, 8),
                "Cliente Teste LTDA",
                "EMP000001",
                "Sr. João",
                "Vendedor Teste",
                br.com.toppower.erp_toppower.sales.salesorder.enums.SalesOrderStatus.ABERTO,
                1500L,           // quotationNumber (origem)
                PaymentCondition.PIX,
                FreightType.CIF,
                new BigDecimal("45.90"),
                null,            // discountType
                null,            // discount
                "Transportadora X",
                new BigDecimal("1000.00"),
                new BigDecimal("1010.45"),
                new BigDecimal("50.00"),
                null,            // notes
                List.of()        // items
        );
        model.put("order", order);
        model.put("clientNameHtml",
                br.com.toppower.erp_toppower.common.util.SoftBreak.name("Cliente Teste LTDA"));
        model.put("attentionHtml",
                br.com.toppower.erp_toppower.common.util.SoftBreak.name("Sr. João"));

        assertProducesValidPdf("pdf/sales-order", model, "pedido de venda");
    }

    @Test
    void renderTechnicalProposalPdf_producesValidPdf() {
        Map<String, Object> model = baseModel();
        // clientType é o enum aninhado TechnicalProposalResponse.ClientType
        // (CUSTOMER/COMPANY). No template o nome dele é resolvido via .name(),
        // então precisamos passar o enum real (não uma String).
        Object clientType =
                br.com.toppower.erp_toppower.sales.technicalproposal.dto
                        .TechnicalProposalResponse.ClientType.CUSTOMER;
        TechnicalProposalLike proposal = new TechnicalProposalLike(
                "PL-001-2026",
                br.com.toppower.erp_toppower.sales.technicalproposal.enums.TechnicalProposalStatus.ABERTA,
                "Cliente Teste LTDA",
                "EMP000001",
                clientType,
                "Eng. Roberto",
                "roberto@aecplataformas.com.br",
                "(19) 99999-9999", // phone
                null,            // address
                List.of(),       // objectives
                null,            // description
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 30),
                null,            // deliveryDate
                List.of(),       // serviceItems
                List.of(),       // productItems
                null,            // discountType
                null,            // discount
                new BigDecimal("45.90"),
                "5 dias",        // deliveryDeadline
                PaymentCondition.PIX,
                "10 dias",       // validity
                FreightType.CIF,
                "Transportadora X",
                new BigDecimal("0.00"),  // servicesSubtotal
                new BigDecimal("0.00"),  // productsSubtotal
                new BigDecimal("0.00"),  // subtotal
                new BigDecimal("0.00"),  // globalDiscountValue
                new BigDecimal("0.00"),  // total
                null             // notes
        );
        model.put("proposal", proposal);

        assertProducesValidPdf("pdf/technical-proposal", model, "proposta técnica");
    }

    /**
     * Modelo base compartilhado entre os 3 templates: IssuerView
     * (cabeçalho) + generatedAt + mapas vazios de produtos.
     *
     * <p>Usa o nome de empresa gigante do bug original (razao social
     * enorme) para garantir que o SoftBreak do IssuerView quebra
     * corretamente e o cabecalho nao invade o titulo do documento.</p>
     */
    private Map<String, Object> baseModel() {
        Map<String, Object> model = new HashMap<>();
        // logoDataUri vazio no teste (sem arquivo de teste) — o template
        // cai no fallback textual "TOP POWER". Para validar a renderização
        // efetiva do logo, crie um teste de integração que use o
        // ImageEmbedder + um logo real em /tmp.
        IssuerView issuer = new IssuerView(
                "AEC PLATAFORMAS ELEVATORIAS E LOCACAO DE EQUIPAMENTOS LTDA",
                "AEC PLATAFORMAS",
                "13.433.616/0001-06",
                "671.137.811.110",
                "29764.01-6",
                "(19) 99999-9999",
                "contato@aecplataformas.com.br",
                null, // logoUrl
                null, // logoDataUri
                List.of("AVENIDA REBOUÇAS, 4465", "RES. VECCON", "SUMARÉ/SP — CEP 13170-700")
        );
        model.put("issuer", issuer);
        model.put("productNames", new HashMap<UUID, String>());
        model.put("productCodes", new HashMap<UUID, String>());
        model.put("generatedAt", Instant.now());
        return model;
    }

    private void assertProducesValidPdf(String template, Map<String, Object> model, String label) {
        byte[] pdf;
        try {
            pdf = service.render(template, model);
        } catch (Exception ex) {
            fail("Falha ao renderizar template de " + label + ": " + ex.getMessage(), ex);
            return;
        }
        assertNotNull(pdf, "PDF nulo para " + label);
        assertTrue(pdf.length > 0, "PDF vazio para " + label);
        // Persiste o PDF em target/ para inspeção visual manual após
        // rodar o teste. Útil para conferir visualmente que o
        // cabeçalho não invade o título (regressão do bug "AEC
        // PLATAFORMAS" sobreposto ao título do documento).
        try {
            String safeName = template.replace("/", "_");
            java.nio.file.Path out = java.nio.file.Paths.get("target", safeName + ".pdf");
            java.nio.file.Files.write(out, pdf);
        } catch (Exception ignored) {
        }
        String header = new String(pdf, 0, Math.min(5, pdf.length));
        assertTrue(header.startsWith("%PDF-"),
                "PDF inválido (sem assinatura %PDF-) para " + label + ": " + header);
    }
}