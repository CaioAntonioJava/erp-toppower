package br.com.toppower.erp_toppower.purchase.parser;

import br.com.toppower.erp_toppower.purchase.dto.NfeInstallmentData;
import br.com.toppower.erp_toppower.purchase.dto.NfeItemData;
import br.com.toppower.erp_toppower.purchase.dto.NfePayableData;
import br.com.toppower.erp_toppower.purchase.dto.NfeSupplierData;
import br.com.toppower.erp_toppower.purchase.exception.NfeImportException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser de XML de NF-e (padrão SEFAZ) usando Jackson XML.
 *
 * <p>Extrai os campos principais necessários para a importação:
 * emitente, identificação, itens, total e duplicatas. Não faz
 * validação fiscal completa — apenas lê os dados para cadastro
 * de fornecedor, produtos, estoque e conta a pagar.</p>
 */
@Component
public class NfeXmlParser {

    private final XmlMapper xmlMapper;

    public NfeXmlParser() {
        // Permite caracteres de controle ilegais (ex.: BEL 0x07) que alguns
        // emissores de NF-e inserem no XML. O Woodstox/Jackson os rejeita
        // por padrão, então habilitamos a leitura leniente. A sanização em
        // PurchaseImportService.readXml remove esses caracteres antes do
        // parse, mas mantemos a feature como rede de segurança.
        this.xmlMapper = XmlMapper.builder()
                .defaultUseWrapper(false)
                .configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true)
                .build();
    }

    // ==================================================================
    // Classes internas que mapeiam a estrutura do XML da NF-e
    // ==================================================================

    @JacksonXmlRootElement(localName = "nfeProc")
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NfeProc {
        @JacksonXmlProperty(localName = "NFe")
        public Nfe nfe;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Nfe {
        @JacksonXmlProperty(localName = "infNFe")
        public InfNfe infNfe;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InfNfe {
        @JacksonXmlProperty(localName = "Id", isAttribute = true)
        public String id; // chave de acesso: NFe + 44 dígitos

        @JacksonXmlProperty(localName = "ide")
        public Ide ide;

        @JacksonXmlProperty(localName = "emit")
        public Emit emit;

        @JacksonXmlProperty(localName = "det")
        @JacksonXmlElementWrapper(localName = "det", useWrapping = false)
        public List<Det> dets = new ArrayList<>();

        @JacksonXmlProperty(localName = "total")
        public Total total;

        @JacksonXmlProperty(localName = "cobr")
        public Cobr cobr;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Ide {
        @JacksonXmlProperty(localName = "nNF")
        public String nNF;

        @JacksonXmlProperty(localName = "serie")
        public String serie;

        @JacksonXmlProperty(localName = "dhEmi")
        public String dhEmi;

        @JacksonXmlProperty(localName = "dEmi")
        public String dEmi; // formato antigo (data sem hora)
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Emit {
        @JacksonXmlProperty(localName = "CNPJ")
        public String cnpj;

        @JacksonXmlProperty(localName = "xNome")
        public String xNome;

        @JacksonXmlProperty(localName = "xFant")
        public String xFant;

        @JacksonXmlProperty(localName = "IE")
        public String ie;

        @JacksonXmlProperty(localName = "IM")
        public String im;

        @JacksonXmlProperty(localName = "enderEmit")
        public EnderEmit enderEmit;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EnderEmit {
        @JacksonXmlProperty(localName = "xLgr")
        public String xLgr;

        @JacksonXmlProperty(localName = "nro")
        public String nro;

        @JacksonXmlProperty(localName = "xCpl")
        public String xCpl;

        @JacksonXmlProperty(localName = "xBairro")
        public String xBairro;

        @JacksonXmlProperty(localName = "xMun")
        public String xMun;

        @JacksonXmlProperty(localName = "UF")
        public String uf;

        @JacksonXmlProperty(localName = "CEP")
        public String cep;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Det {
        @JacksonXmlProperty(localName = "nItem", isAttribute = true)
        public String nItem;

        @JacksonXmlProperty(localName = "prod")
        public Prod prod;

        @JacksonXmlProperty(localName = "imposto")
        public Imposto imposto;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Prod {
        @JacksonXmlProperty(localName = "cProd")
        public String cProd;

        @JacksonXmlProperty(localName = "cEAN")
        public String cEAN;

        @JacksonXmlProperty(localName = "xProd")
        public String xProd;

        @JacksonXmlProperty(localName = "NCM")
        public String ncm;

        @JacksonXmlProperty(localName = "CEST")
        public String cest;

        @JacksonXmlProperty(localName = "uCom")
        public String uCom;

        @JacksonXmlProperty(localName = "qCom")
        public String qCom;

        @JacksonXmlProperty(localName = "vUnCom")
        public String vUnCom;

        @JacksonXmlProperty(localName = "vProd")
        public String vProd;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Imposto {
        @JacksonXmlProperty(localName = "ICMS")
        public Icms icms;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Icms {
        // ICMS00, ICMS10, ICMS20, ICMS40, ICMS51, ICMS60, ICMS70, ICMS90, etc.
        // Cada CST tem seu próprio sub-elemento, mas todos contêm o campo orig.
        // Como só precisamos do orig, usamos um único campo que o Jackson
        // tentará casar — mas como orig está dentro do sub-tipo (ex.: ICMS00),
        // precisamos de uma classe intermediária. Jackson com defaultUseWrapper(false)
        // e @JsonIgnoreProperties(ignoreUnknown=true) ignora o CST e lê o primeiro
        // filho que encontrar.
        @JacksonXmlProperty(localName = "orig")
        public String orig;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Total {
        @JacksonXmlProperty(localName = "ICMSTot")
        public IcmsTot icmsTot;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IcmsTot {
        @JacksonXmlProperty(localName = "vNF")
        public String vNF;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Cobr {
        @JacksonXmlProperty(localName = "dup")
        @JacksonXmlElementWrapper(localName = "dup", useWrapping = false)
        public List<Dup> dups;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Dup {
        @JacksonXmlProperty(localName = "nDup")
        public String nDup;

        @JacksonXmlProperty(localName = "dVenc")
        public String dVenc;

        @JacksonXmlProperty(localName = "vDup")
        public String vDup;
    }

    // ==================================================================
    // Métodos públicos de parse
    // ==================================================================

    /**
     * Parseia o XML da NF-e e retorna os dados estruturados.
     *
     * @param xmlContent conteúdo do arquivo XML como String
     * @return objeto {@link ParsedNfe} com os dados extraídos
     * @throws NfeImportException se o parse falhar
     */
    public ParsedNfe parse(String xmlContent) {
        try {
            NfeProc proc = xmlMapper.readValue(xmlContent, NfeProc.class);
            if (proc == null || proc.nfe == null || proc.nfe.infNfe == null) {
                throw new NfeImportException("XML inválido: estrutura NF-e não encontrada.");
            }
            InfNfe inf = proc.nfe.infNfe;
            return new ParsedNfe(
                    extractAccessKey(inf.id),
                    inf.ide != null ? inf.ide.nNF : null,
                    inf.ide != null ? inf.ide.serie : null,
                    extractIssueDate(inf.ide),
                    extractSupplier(inf.emit),
                    extractItems(inf.dets),
                    extractTotalValue(inf.total),
                    extractInstallments(inf.cobr)
            );
        } catch (NfeImportException e) {
            throw e;
        } catch (Exception e) {
            throw new NfeImportException("Falha ao parsear XML da NF-e: " + e.getMessage(), e);
        }
    }

    private String extractAccessKey(String id) {
        if (id == null || id.isBlank()) return null;
        // O Id vem como "NFe" + 44 dígitos da chave de acesso.
        return id.startsWith("NFe") ? id.substring(3) : id;
    }

    private LocalDate extractIssueDate(Ide ide) {
        if (ide == null) return LocalDate.now();
        String dateStr = ide.dhEmi != null ? ide.dhEmi : ide.dEmi;
        if (dateStr == null || dateStr.isBlank()) return LocalDate.now();
        try {
            // dhEmi: 2024-01-15T10:30:00-03:00 — extrair só a data.
            // dEmi: 2024-01-15
            String datePart = dateStr.length() > 10 ? dateStr.substring(0, 10) : dateStr;
            return LocalDate.parse(datePart, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    private NfeSupplierData extractSupplier(Emit emit) {
        if (emit == null) {
            throw new NfeImportException("XML inválido: emitente (emit) não encontrado.");
        }
        String cnpj = emit.cnpj != null ? emit.cnpj.replaceAll("\\D", "") : null;
        if (cnpj == null || cnpj.isBlank()) {
            throw new NfeImportException("XML inválido: CNPJ do emitente não encontrado.");
        }
        EnderEmit end = emit.enderEmit;
        return new NfeSupplierData(
                false, // existing — será resolvido pelo service
                null,  // id — será resolvido pelo service
                cnpj,
                emit.xNome != null ? emit.xNome.trim() : "",
                emit.xFant,
                emit.ie,
                emit.im,
                end != null ? end.xLgr : null,
                end != null ? (end.nro != null ? end.nro : "S/N") : "S/N",
                end != null ? end.xCpl : null,
                end != null ? end.xBairro : null,
                end != null ? end.xMun : null,
                end != null ? end.uf : null,
                end != null && end.cep != null ? formatCep(end.cep) : null
        );
    }

    private String formatCep(String cep) {
        String digits = cep.replaceAll("\\D", "");
        if (digits.length() == 8) {
            return digits.substring(0, 5) + "-" + digits.substring(5);
        }
        return cep;
    }

    private List<NfeItemData> extractItems(List<Det> dets) {
        if (dets == null || dets.isEmpty()) {
            throw new NfeImportException("XML inválido: nenhum item (det) encontrado na NF-e.");
        }
        List<NfeItemData> items = new ArrayList<>(dets.size());
        for (Det det : dets) {
            Prod prod = det.prod;
            if (prod == null) continue;
            String origem = null;
            if (det.imposto != null && det.imposto.icms != null) {
                origem = det.imposto.icms.orig;
            }
            items.add(new NfeItemData(
                    null, // status — resolvido pelo service
                    null, // productId — resolvido pelo service
                    prod.cProd,
                    isNotEmpty(prod.cEAN) && !prod.cEAN.equals("SEM GTIN") ? prod.cEAN : null,
                    prod.xProd != null ? prod.xProd.trim() : "",
                    prod.ncm,
                    prod.cest,
                    prod.uCom,
                    parseDecimal(prod.qCom),
                    parseDecimal(prod.vUnCom),
                    parseDecimal(prod.vProd),
                    origem,
                    null, // pesoLiquido — extraído do transp/vol se necessário
                    null  // pesoBruto
            ));
        }
        return items;
    }

    private BigDecimal extractTotalValue(Total total) {
        if (total == null || total.icmsTot == null || total.icmsTot.vNF == null) {
            throw new NfeImportException("XML inválido: valor total (vNF) não encontrado.");
        }
        return parseDecimal(total.icmsTot.vNF);
    }

    private List<NfeInstallmentData> extractInstallments(Cobr cobr) {
        if (cobr == null || cobr.dups == null || cobr.dups.isEmpty()) {
            return List.of(); // à vista — sem parcelas
        }
        List<NfeInstallmentData> installments = new ArrayList<>(cobr.dups.size());
        for (Dup dup : cobr.dups) {
            installments.add(new NfeInstallmentData(
                    dup.nDup,
                    parseDate(dup.dVenc),
                    parseDecimal(dup.vDup)
            ));
        }
        return installments;
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return LocalDate.now();
        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    private BigDecimal parseDecimal(String value) {
        if (value == null || value.isBlank()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(value.trim().replace(",", "."));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private boolean isNotEmpty(String s) {
        return s != null && !s.isBlank();
    }

    // ==================================================================
    // Resultado do parse
    // ==================================================================

    /**
     * Dados extraídos do XML da NF-e, prontos para uso pelo service.
     */
    public record ParsedNfe(
            String accessKey,
            String nNF,
            String serie,
            LocalDate issueDate,
            NfeSupplierData supplier,
            List<NfeItemData> items,
            BigDecimal totalValue,
            List<NfeInstallmentData> installments
    ) {
        /**
         * Número formatado da nota para idempotência: "nNF/serie".
         */
        public String invoiceNumber() {
            String n = nNF != null ? nNF : "?";
            String s = serie != null ? serie : "0";
            return n + "/" + s;
        }

        /**
         * Descrição padrão para a conta a pagar.
         */
        public String payableDescription() {
            return "NF-e " + invoiceNumber() + " - " + (supplier != null ? supplier.legalName() : "");
        }
    }
}