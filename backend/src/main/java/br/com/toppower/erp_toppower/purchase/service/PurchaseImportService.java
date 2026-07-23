package br.com.toppower.erp_toppower.purchase.service;

import br.com.toppower.erp_toppower.common.context.OrganizationContext;
import br.com.toppower.erp_toppower.common.embeddable.Address;
import br.com.toppower.erp_toppower.payable.entity.Payable;
import br.com.toppower.erp_toppower.payable.entity.PayableInstallment;
import br.com.toppower.erp_toppower.payable.enums.PayableSource;
import br.com.toppower.erp_toppower.payable.enums.PayableStatus;
import br.com.toppower.erp_toppower.payable.repository.PayableInstallmentRepository;
import br.com.toppower.erp_toppower.payable.repository.PayableRepository;
import br.com.toppower.erp_toppower.product.entity.Product;
import br.com.toppower.erp_toppower.product.enums.OrigemProduto;
import br.com.toppower.erp_toppower.product.enums.ProductStatus;
import br.com.toppower.erp_toppower.product.enums.UnitType;
import br.com.toppower.erp_toppower.product.repository.ProductRepository;
import br.com.toppower.erp_toppower.purchase.dto.ItemAction;
import br.com.toppower.erp_toppower.purchase.dto.ItemStatus;
import br.com.toppower.erp_toppower.purchase.dto.NfeConfirmItem;
import br.com.toppower.erp_toppower.purchase.dto.NfeConfirmResponse;
import br.com.toppower.erp_toppower.purchase.dto.NfeItemData;
import br.com.toppower.erp_toppower.purchase.dto.NfePayableData;
import br.com.toppower.erp_toppower.purchase.dto.NfePreviewResponse;
import br.com.toppower.erp_toppower.purchase.dto.NfeSupplierData;
import br.com.toppower.erp_toppower.purchase.entity.ProductSupplierCode;
import br.com.toppower.erp_toppower.purchase.exception.NfeImportException;
import br.com.toppower.erp_toppower.purchase.parser.NfeXmlParser;
import br.com.toppower.erp_toppower.purchase.repository.ProductSupplierCodeRepository;
import br.com.toppower.erp_toppower.stock.enums.MovementSource;
import br.com.toppower.erp_toppower.stock.enums.MovementType;
import br.com.toppower.erp_toppower.stock.repository.StockMovementRepository;
import br.com.toppower.erp_toppower.stock.service.StockService;
import br.com.toppower.erp_toppower.supplier.entity.Supplier;
import br.com.toppower.erp_toppower.supplier.enums.SupplierStatus;
import br.com.toppower.erp_toppower.supplier.repository.SupplierRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Orquestra a importação de NF-e (XML de nota de compra/entrada).
 *
 * <p>Fluxo em 2 passos:</p>
 * <ol>
 *   <li>{@link #preview(MultipartFile)} — parseia o XML e retorna um
 *       resumo sem persistir nada, com classificação de itens por
 *       similaridade (fornecedor/EAN/código/nome) e detecção de
 *       duplicidade pela Chave de Acesso;</li>
 *   <li>{@link #confirm(String, List)} — re-parseia e efetiva: cria
 *       fornecedor (se novo), produtos (se novos), entrada de estoque
 *       e conta a pagar, respeitando as decisões do usuário por item.</li>
 * </ol>
 *
 * <p><b>Regras de negócio</b>:</p>
 * <ul>
 *   <li>A Chave de Acesso da NF-e é única nacionalmente — a mesma nota
 *       nunca pode ser importada duas vezes (idempotência primária).</li>
 *   <li>O fornecedor é sempre determinado pelo CNPJ do emitente no XML;
 *       o usuário não pode atribuir a outro fornecedor.</li>
 *   <li>Produtos existentes apenas recebem entrada de estoque; produtos
 *       novos são cadastrados. A relação produto↔fornecedor↔cProd é
 *       persistida para acelerar matchings futuros.</li>
 * </ul>
 */
@Service
public class PurchaseImportService {

    private static final Logger log = LoggerFactory.getLogger(PurchaseImportService.class);

    private final NfeXmlParser parser;
    private final NfeProductMatcher matcher;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final ProductSupplierCodeRepository productSupplierCodeRepository;
    private final PayableRepository payableRepository;
    private final PayableInstallmentRepository installmentRepository;
    private final StockMovementRepository stockMovementRepository;
    private final StockService stockService;

    public PurchaseImportService(NfeXmlParser parser,
                                 NfeProductMatcher matcher,
                                 SupplierRepository supplierRepository,
                                 ProductRepository productRepository,
                                 ProductSupplierCodeRepository productSupplierCodeRepository,
                                 PayableRepository payableRepository,
                                 PayableInstallmentRepository installmentRepository,
                                 StockMovementRepository stockMovementRepository,
                                 StockService stockService) {
        this.parser = parser;
        this.matcher = matcher;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.productSupplierCodeRepository = productSupplierCodeRepository;
        this.payableRepository = payableRepository;
        this.installmentRepository = installmentRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.stockService = stockService;
    }

    // ==================================================================
    // Passo 1: Preview (não persiste)
    // ==================================================================

    /**
     * Parseia o XML da NF-e e retorna um preview sem persistir nada.
     * Classifica cada item por similaridade e detecta duplicidade pela
     * Chave de Acesso.
     */
    public NfePreviewResponse preview(MultipartFile file) {
        requireOrganizationContext();
        String xml = readXml(file);
        NfeXmlParser.ParsedNfe nfe = parser.parse(xml);

        // Valida a Chave de Acesso — sem ela não há idempotência confiável.
        String accessKey = nfe.accessKey();
        if (accessKey == null || accessKey.isBlank()) {
            throw new NfeImportException("Chave de acesso da NF-e não encontrada no XML.");
        }

        // Detecta duplicidade pela Chave de Acesso.
        boolean alreadyImported =
                payableRepository.findActiveByPurchaseInvoiceAccessKey(accessKey).isPresent();

        // Identifica fornecedor (sem criar) e resolve o ID se existir.
        NfeSupplierData supplierData = resolveSupplierPreview(nfe.supplier());
        Long supplierId = supplierData.existing() ? supplierData.id() : null;

        // Classifica itens (sem criar) usando o matcher.
        List<NfeItemData> items = resolveItemsPreview(nfe.items(), supplierId);

        // Monta dados da conta a pagar.
        NfePayableData payableData = buildPayableData(nfe);

        String xmlBase64 = Base64.getEncoder().encodeToString(xml.getBytes(StandardCharsets.UTF_8));

        return new NfePreviewResponse(xmlBase64, alreadyImported, accessKey,
                supplierData, items, payableData);
    }

    // ==================================================================
    // Passo 2: Confirm (persiste tudo)
    // ==================================================================

    /**
     * Re-parseia o XML e efetiva a importação: cria fornecedor, produtos
     * (conforme decisão do usuário), entrada de estoque e conta a pagar.
     * Idempotente pela Chave de Acesso.
     */
    @Transactional
    public NfeConfirmResponse confirm(String xmlBase64, List<NfeConfirmItem> itemDecisions) {
        requireOrganizationContext();
        String xml = new String(Base64.getDecoder().decode(xmlBase64), StandardCharsets.UTF_8);
        NfeXmlParser.ParsedNfe nfe = parser.parse(xml);
        String invoiceNumber = nfe.invoiceNumber();
        String accessKey = nfe.accessKey();

        // Valida a Chave de Acesso.
        if (accessKey == null || accessKey.isBlank()) {
            throw new NfeImportException("Chave de acesso da NF-e não encontrada no XML.");
        }

        // Idempotência primária pela Chave de Acesso (única nacionalmente).
        if (payableRepository.findActiveByPurchaseInvoiceAccessKey(accessKey).isPresent()) {
            throw new NfeImportException(
                    "NF-e com chave de acesso " + accessKey + " já foi importada.");
        }
        // Fallback por número da nota (proteção adicional).
        if (payableRepository.findActiveByPurchaseInvoiceNumber(invoiceNumber).isPresent()) {
            throw new NfeImportException("NF-e " + invoiceNumber + " já foi importada.");
        }
        if (stockMovementRepository.existsBySourceNumberAndSourceAndTypeAndReversedFalse(
                invoiceNumber, MovementSource.NFE_IMPORT, MovementType.ENTRADA)) {
            throw new NfeImportException("NF-e " + invoiceNumber + " já teve entrada de estoque registrada.");
        }

        // 1. Cria ou reutiliza fornecedor (sempre pelo CNPJ do emitente).
        Supplier supplier = resolveOrCreateSupplier(nfe.supplier());
        boolean supplierCreated = supplier.getId() == null
                || supplierRepository.findById(supplier.getId()).isEmpty();
        if (supplier.getId() == null) {
            supplier = supplierRepository.save(supplier);
        }

        // 2. Monta mapa de decisões por itemIndex.
        Map<Integer, NfeConfirmItem> decisions = new HashMap<>();
        for (NfeConfirmItem d : itemDecisions) {
            decisions.put(d.itemIndex(), d);
        }

        // 3. Processa cada item conforme a decisão do usuário.
        List<Long> createdProductIds = new ArrayList<>();
        List<Long> existingProductIds = new ArrayList<>();
        int ignoredItemCount = 0;
        int index = 0;
        for (NfeItemData item : nfe.items()) {
            int itemIndex = index++;
            NfeConfirmItem decision = decisions.get(itemIndex);
            if (decision == null) {
                throw new NfeImportException(
                        "Decisão ausente para o item " + itemIndex + " da nota.");
            }

            switch (decision.action()) {
                case IGNORAR -> ignoredItemCount++;
                case CADASTRAR -> {
                    Product product = createProduct(item);
                    registrarEntrada(product.getId(), item, invoiceNumber, itemIndex);
                    upsertSupplierCode(product.getId(), supplier.getId(), item.code());
                    createdProductIds.add(product.getId());
                }
                case ESTOQUE -> {
                    Long productId = resolveExistingProductId(decision, itemIndex);
                    registrarEntrada(productId, item, invoiceNumber, itemIndex);
                    upsertSupplierCode(productId, supplier.getId(), item.code());
                    existingProductIds.add(productId);
                }
            }
        }

        // 4. Cria conta a pagar com parcelas.
        Payable payable = createPayable(nfe, supplier.getId(), invoiceNumber, accessKey);

        log.info("NF-e {} (chave {}) importada: fornecedor={}, produtos criados={}, "
                        + "existentes={}, ignorados={}, conta a pagar={}",
                invoiceNumber, accessKey, supplier.getId(), createdProductIds.size(),
                existingProductIds.size(), ignoredItemCount, payable.getId());

        return new NfeConfirmResponse(
                supplier.getId(),
                supplierCreated,
                createdProductIds,
                existingProductIds,
                payable.getId(),
                invoiceNumber,
                accessKey,
                ignoredItemCount
        );
    }

    // ==================================================================
    // Helpers — Fornecedor
    // ==================================================================

    private NfeSupplierData resolveSupplierPreview(NfeSupplierData raw) {
        String normalizedTaxId = normalizeCnpj(raw.taxId());
        Optional<Supplier> existing = supplierRepository.findByTaxId(normalizedTaxId);
        if (existing.isPresent()) {
            Supplier s = existing.get();
            return new NfeSupplierData(
                    true, s.getId(), s.getTaxId(), s.getLegalName(), s.getTradeName(),
                    s.getStateRegistration(), s.getMunicipalRegistration(),
                    raw.street(), raw.number(), raw.complement(), raw.neighborhood(),
                    raw.city(), raw.state(), raw.zipCode()
            );
        }
        return raw;
    }

    private Supplier resolveOrCreateSupplier(NfeSupplierData data) {
        String normalizedTaxId = normalizeCnpj(data.taxId());
        Optional<Supplier> existing = supplierRepository.findByTaxId(normalizedTaxId);
        if (existing.isPresent()) {
            return existing.get();
        }
        // Cria novo fornecedor com dados do emitente.
        Supplier supplier = new Supplier();
        supplier.setLegalName(data.legalName());
        supplier.setTradeName(data.tradeName());
        supplier.setTaxId(normalizedTaxId);
        supplier.setStateRegistration(data.stateRegistration());
        supplier.setMunicipalRegistration(data.municipalRegistration());
        supplier.setEmail("importado@nfe.com.br");
        supplier.setPhone(null);
        supplier.setContactName(null);
        supplier.setStatus(SupplierStatus.ATIVO);
        Address address = new Address();
        address.setStreet(data.street() != null ? data.street() : "Não informado");
        address.setNumber(data.number() != null ? data.number() : "S/N");
        address.setComplement(data.complement());
        address.setNeighborhood(data.neighborhood());
        address.setCity(data.city() != null ? data.city() : "Não informado");
        address.setState(data.state() != null ? data.state() : "SP");
        address.setZipCode(data.zipCode());
        supplier.setAddress(address);
        return supplier;
    }

    // ==================================================================
    // Helpers — Produtos (preview)
    // ==================================================================

    private List<NfeItemData> resolveItemsPreview(List<NfeItemData> rawItems, Long supplierId) {
        List<NfeItemData> result = new ArrayList<>(rawItems.size());
        for (NfeItemData item : rawItems) {
            NfeProductMatcher.MatchResult m = matcher.match(
                    supplierId, item.code(), item.codigoBarras(), item.name(), item.ncm());
            result.add(new NfeItemData(
                    m.status(),
                    m.status() != ItemStatus.NOVO ? m.productId() : null,
                    item.itemIndex(),
                    m.matchReason(),
                    m.status() != ItemStatus.NOVO ? m.productId() : null,
                    m.existingProductName(),
                    item.code(), item.codigoBarras(), item.name(),
                    item.ncm(), item.cest(), item.unit(),
                    item.quantity(), item.unitValue(), item.totalValue(),
                    item.origem(), item.pesoLiquido(), item.pesoBruto()
            ));
        }
        return result;
    }

    // ==================================================================
    // Helpers — Produtos (confirm)
    // ==================================================================

    /**
     * Cria um novo produto a partir dos dados fiscais do item da NF-e.
     */
    private Product createProduct(NfeItemData item) {
        Product product = new Product();
        product.setName(item.name());
        product.setCode(item.code());
        product.setCodigoBarras(item.codigoBarras());
        product.setNcm(item.ncm());
        product.setCest(item.cest());
        product.setUnitType(parseUnitType(item.unit()));
        product.setOrigem(parseOrigem(item.origem()));
        product.setPrice(item.unitValue() != null ? item.unitValue() : BigDecimal.ZERO);
        product.setStockQuantity(BigDecimal.ZERO); // será atualizado pelo StockService
        product.setStatus(ProductStatus.ATIVO);
        return productRepository.save(product);
    }

    /**
     * Resolve o ID do produto existente para a ação ESTOQUE. Para itens
     * EXISTENTE, o produto é determinístico (não precisa de
     * existingProductId). Para DIVERGENTE, o usuário deve informar o
     * existingProductId (o candidato sugerido).
     */
    private Long resolveExistingProductId(NfeConfirmItem decision, int itemIndex) {
        Long productId = decision.existingProductId();
        if (productId == null) {
            throw new NfeImportException(
                    "ID do produto existente é obrigatório para ação ESTOQUE do item " + itemIndex + ".");
        }
        // Valida existência e pertencimento à org (o filtro org é automático).
        if (productRepository.findById(productId).isEmpty()) {
            throw new NfeImportException(
                    "Produto " + productId + " informado no item " + itemIndex
                            + " não encontrado na organização.");
        }
        return productId;
    }

    /**
     * Registra a entrada de estoque para um item da NF-e.
     */
    private void registrarEntrada(Long productId, NfeItemData item,
                                  String invoiceNumber, int itemIndex) {
        stockService.registrarEntrada(
                productId,
                item.quantity(),
                MovementSource.NFE_IMPORT,
                0L, // sourceId — usamos sourceNumber para idempotência
                invoiceNumber,
                "Entrada por NF-e " + invoiceNumber + " - item " + itemIndex
                        + (item.code() != null ? " (" + item.code() + ")" : "")
        );
    }

    /**
     * Upsert da relação produto↔fornecedor↔cProd. Se já existe, não
     * duplica. Garante que importações futuras do mesmo fornecedor
     * casem instantaneamente pelo código do produto no fornecedor.
     */
    private void upsertSupplierCode(Long productId, Long supplierId, String supplierCode) {
        if (supplierId == null || supplierCode == null || supplierCode.isBlank()) {
            return;
        }
        String code = supplierCode.trim();
        if (productSupplierCodeRepository.existsBySupplierIdAndSupplierCode(supplierId, code)) {
            return;
        }
        ProductSupplierCode rel = new ProductSupplierCode();
        rel.setProductId(productId);
        rel.setSupplierId(supplierId);
        rel.setSupplierCode(code);
        productSupplierCodeRepository.save(rel);
    }

    // ==================================================================
    // Helpers — Conta a pagar
    // ==================================================================

    private NfePayableData buildPayableData(NfeXmlParser.ParsedNfe nfe) {
        return new NfePayableData(
                nfe.totalValue(),
                nfe.issueDate(),
                nfe.payableDescription(),
                nfe.invoiceNumber(),
                nfe.accessKey(),
                nfe.installments()
        );
    }

    private Payable createPayable(NfeXmlParser.ParsedNfe nfe, Long supplierId,
                                  String invoiceNumber, String accessKey) {
        Payable payable = new Payable();
        payable.setDescription(nfe.payableDescription());
        payable.setValue(nfe.totalValue());
        payable.setPaidAmount(BigDecimal.ZERO);
        payable.setIssueDate(nfe.issueDate());
        payable.setSupplierId(supplierId);
        payable.setSourceType(PayableSource.PURCHASE_INVOICE);
        payable.setPurchaseInvoiceNumber(invoiceNumber);
        payable.setPurchaseInvoiceAccessKey(accessKey);
        payable.setStatus(PayableStatus.ABERTO);

        // Define vencimento-base: primeira duplicata ou data de emissão.
        LocalDate baseDueDate = nfe.issueDate();
        if (!nfe.installments().isEmpty()) {
            baseDueDate = nfe.installments().get(0).dueDate();
        }
        payable.setDueDate(baseDueDate);
        payable.setInstallmentsCount(Math.max(1, nfe.installments().size()));

        Payable saved = payableRepository.save(payable);

        // Cria parcelas.
        if (nfe.installments().isEmpty()) {
            // À vista: parcela única com vencimento = data de emissão.
            PayableInstallment inst = new PayableInstallment();
            inst.setPayableId(saved.getId());
            inst.setInstallmentNumber(1);
            inst.setAmount(nfe.totalValue());
            inst.setDueDate(nfe.issueDate());
            inst.setPaidAmount(BigDecimal.ZERO);
            inst.setStatus(PayableStatus.ABERTO);
            installmentRepository.save(inst);
        } else {
            int num = 1;
            for (var dup : nfe.installments()) {
                PayableInstallment inst = new PayableInstallment();
                inst.setPayableId(saved.getId());
                inst.setInstallmentNumber(num++);
                inst.setAmount(dup.amount());
                inst.setDueDate(dup.dueDate());
                inst.setPaidAmount(BigDecimal.ZERO);
                inst.setStatus(PayableStatus.ABERTO);
                installmentRepository.save(inst);
            }
        }

        return saved;
    }

    // ==================================================================
    // Helpers — utilitários
    // ==================================================================

    /**
     * Garante que há uma Organization ativa na requisição antes de prosseguir
     * com a importação. Sem isso, o {@code OrganizationEntityListener} deixa
     * {@code organization_id = NULL} no Payable criado, e o Hibernate filter
     * exclui o registro de todas as listagens scoped (ele nunca aparece no
     * dashboard da organização).
     */
    private void requireOrganizationContext() {
        if (OrganizationContext.get() == null) {
            throw new NfeImportException(
                    "Selecione uma organização antes de importar a NF-e.");
        }
    }

    private String readXml(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new NfeImportException("Arquivo XML vazio ou não enviado.");
        }
        try {
            String xml = new String(file.getBytes(), StandardCharsets.UTF_8);
            // Remove BOM UTF-8 (EF BB BF) que alguns emissores inserem e
            // caracteres de controle ilegais (0x00-0x08, 0x0B, 0x0C,
            // 0x0E-0x1F) que o Jackson/Woodstox rejeita por padrão.
            // Preserva tab (\t), LF (\n) e CR (\r), válidos em XML.
            if (xml != null && !xml.isEmpty() && xml.charAt(0) == '\uFEFF') {
                xml = xml.substring(1);
            }
            xml = xml.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "");
            return xml;
        } catch (IOException e) {
            throw new NfeImportException("Falha ao ler o arquivo XML: " + e.getMessage(), e);
        }
    }

    private String normalizeCnpj(String cnpj) {
        if (cnpj == null) return null;
        return cnpj.replaceAll("\\D", "");
    }

    private UnitType parseUnitType(String unit) {
        if (unit == null || unit.isBlank()) {
            return UnitType.UN;
        }
        try {
            return UnitType.valueOf(unit.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unidade da NF-e não reconhecida: {}. Usando UN como fallback.", unit);
            return UnitType.UN;
        }
    }

    private OrigemProduto parseOrigem(String orig) {
        if (orig == null || orig.isBlank()) {
            return OrigemProduto.NACIONAL;
        }
        try {
            int code = Integer.parseInt(orig.trim());
            return switch (code) {
                case 0 -> OrigemProduto.NACIONAL;
                case 1 -> OrigemProduto.ESTRANGEIRA_IMPORTACAO_DIRETA;
                case 2 -> OrigemProduto.ESTRANGEIRA_ADQUIRIDA_MERCADO_INTERNO;
                case 3 -> OrigemProduto.NACIONAL_IMPORTACAO_SUPERIOR_40;
                case 4 -> OrigemProduto.NACIONAL_PROCESSOS_PRODUTIVOS_BASICOS;
                case 5 -> OrigemProduto.NACIONAL_IMPORTACAO_SUPERIOR_70;
                case 6 -> OrigemProduto.ESTRANGEIRA_IMPORTACAO_DIRETA_SEM_SIMILAR;
                case 7 -> OrigemProduto.ESTRANGEIRA_ADQUIRIDA_MERCADO_INTERNO_SEM_SIMILAR;
                case 8 -> OrigemProduto.NACIONAL_IMPORTACAO_ACIMA_70;
                default -> OrigemProduto.NACIONAL;
            };
        } catch (NumberFormatException e) {
            return OrigemProduto.NACIONAL;
        }
    }
}