package br.com.toppower.erp_toppower.purchase.service;

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
import br.com.toppower.erp_toppower.purchase.dto.ItemStatus;
import br.com.toppower.erp_toppower.purchase.dto.NfeConfirmResponse;
import br.com.toppower.erp_toppower.purchase.dto.NfeItemData;
import br.com.toppower.erp_toppower.purchase.dto.NfePayableData;
import br.com.toppower.erp_toppower.purchase.dto.NfePreviewResponse;
import br.com.toppower.erp_toppower.purchase.dto.NfeSupplierData;
import br.com.toppower.erp_toppower.purchase.exception.NfeImportException;
import br.com.toppower.erp_toppower.purchase.parser.NfeXmlParser;
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
import java.util.List;
import java.util.Optional;

/**
 * Orquestra a importação de NF-e (XML de nota de compra/entrada).
 *
 * <p>Fluxo em 2 passos:</p>
 * <ol>
 *   <li>{@link #preview(MultipartFile)} — parseia o XML e retorna um
 *       resumo sem persistir nada;</li>
 *   <li>{@link #confirm(String)} — re-parseia e efetiva: cria fornecedor
 *       (se novo), produtos (se novos), entrada de estoque e conta a pagar.</li>
 * </ol>
 */
@Service
public class PurchaseImportService {

    private static final Logger log = LoggerFactory.getLogger(PurchaseImportService.class);

    private final NfeXmlParser parser;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final PayableRepository payableRepository;
    private final PayableInstallmentRepository installmentRepository;
    private final StockMovementRepository stockMovementRepository;
    private final StockService stockService;

    public PurchaseImportService(NfeXmlParser parser,
                                 SupplierRepository supplierRepository,
                                 ProductRepository productRepository,
                                 PayableRepository payableRepository,
                                 PayableInstallmentRepository installmentRepository,
                                 StockMovementRepository stockMovementRepository,
                                 StockService stockService) {
        this.parser = parser;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
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
     */
    public NfePreviewResponse preview(MultipartFile file) {
        String xml = readXml(file);
        NfeXmlParser.ParsedNfe nfe = parser.parse(xml);

        // Identifica fornecedor (sem criar).
        NfeSupplierData supplierData = resolveSupplierPreview(nfe.supplier());

        // Classifica itens (sem criar).
        List<NfeItemData> items = resolveItemsPreview(nfe.items());

        // Monta dados da conta a pagar.
        NfePayableData payableData = buildPayableData(nfe);

        String xmlBase64 = Base64.getEncoder().encodeToString(xml.getBytes(StandardCharsets.UTF_8));

        return new NfePreviewResponse(xmlBase64, supplierData, items, payableData);
    }

    // ==================================================================
    // Passo 2: Confirm (persiste tudo)
    // ==================================================================

    /**
     * Re-parseia o XML e efetiva a importação: cria fornecedor, produtos,
     * entrada de estoque e conta a pagar.
     */
    @Transactional
    public NfeConfirmResponse confirm(String xmlBase64) {
        String xml = new String(Base64.getDecoder().decode(xmlBase64), StandardCharsets.UTF_8);
        NfeXmlParser.ParsedNfe nfe = parser.parse(xml);
        String invoiceNumber = nfe.invoiceNumber();

        // Idempotência: verifica se a nota já foi importada.
        if (payableRepository.findActiveByPurchaseInvoiceNumber(invoiceNumber).isPresent()) {
            throw new NfeImportException("NF-e " + invoiceNumber + " já foi importada.");
        }
        if (stockMovementRepository.existsBySourceNumberAndSourceAndTypeAndReversedFalse(
                invoiceNumber, MovementSource.NFE_IMPORT, MovementType.ENTRADA)) {
            throw new NfeImportException("NF-e " + invoiceNumber + " já teve entrada de estoque registrada.");
        }

        // 1. Cria ou reutiliza fornecedor.
        Supplier supplier = resolveOrCreateSupplier(nfe.supplier());
        boolean supplierCreated = supplier.getId() == null
                || supplierRepository.findById(supplier.getId()).isEmpty();
        if (supplier.getId() == null) {
            supplier = supplierRepository.save(supplier);
        }

        // 2. Cria produtos novos e registra entrada de estoque para todos.
        List<Long> createdProductIds = new ArrayList<>();
        List<Long> existingProductIds = new ArrayList<>();
        for (NfeItemData item : nfe.items()) {
            Product product = resolveOrCreateProduct(item);
            boolean isNew = product.getId() == null;
            if (isNew) {
                product = productRepository.save(product);
                createdProductIds.add(product.getId());
            } else {
                existingProductIds.add(product.getId());
            }

            // Registra entrada de estoque.
            stockService.registrarEntrada(
                    product.getId(),
                    item.quantity(),
                    MovementSource.NFE_IMPORT,
                    0L, // sourceId — usamos sourceNumber para idempotência
                    invoiceNumber,
                    "Entrada por NF-e " + invoiceNumber + " - item " + item.code()
            );
        }

        // 3. Cria conta a pagar com parcelas.
        Payable payable = createPayable(nfe, supplier.getId(), invoiceNumber);

        log.info("NF-e {} importada: fornecedor={}, produtos criados={}, existentes={}, conta a pagar={}",
                invoiceNumber, supplier.getId(), createdProductIds.size(),
                existingProductIds.size(), payable.getId());

        return new NfeConfirmResponse(
                supplier.getId(),
                supplierCreated,
                createdProductIds,
                existingProductIds,
                payable.getId(),
                invoiceNumber
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
    // Helpers — Produtos
    // ==================================================================

    private List<NfeItemData> resolveItemsPreview(List<NfeItemData> rawItems) {
        List<NfeItemData> result = new ArrayList<>(rawItems.size());
        for (NfeItemData item : rawItems) {
            ItemStatus status = classifyItem(item);
            Long productId = null;
            if (status != ItemStatus.NOVO) {
                productId = findExistingProduct(item).map(Product::getId).orElse(null);
            }
            result.add(new NfeItemData(
                    status, productId,
                    item.code(), item.codigoBarras(), item.name(),
                    item.ncm(), item.cest(), item.unit(),
                    item.quantity(), item.unitValue(), item.totalValue(),
                    item.origem(), item.pesoLiquido(), item.pesoBruto()
            ));
        }
        return result;
    }

    private ItemStatus classifyItem(NfeItemData item) {
        Optional<Product> existing = findExistingProduct(item);
        if (existing.isEmpty()) {
            return ItemStatus.NOVO;
        }
        // Compara nome: se divergente, marca como DIVERGENTE.
        Product p = existing.get();
        if (p.getName() != null && !p.getName().equalsIgnoreCase(item.name())) {
            return ItemStatus.DIVERGENTE;
        }
        return ItemStatus.EXISTENTE;
    }

    private Optional<Product> findExistingProduct(NfeItemData item) {
        // Primeiro por código (cProd), depois por código de barras (cEAN).
        if (item.code() != null && !item.code().isBlank()) {
            Optional<Product> byCode = productRepository.findByCode(item.code());
            if (byCode.isPresent()) return byCode;
        }
        if (item.codigoBarras() != null && !item.codigoBarras().isBlank()) {
            return productRepository.findByCodigoBarras(item.codigoBarras());
        }
        return Optional.empty();
    }

    private Product resolveOrCreateProduct(NfeItemData item) {
        Optional<Product> existing = findExistingProduct(item);
        if (existing.isPresent()) {
            return existing.get();
        }
        // Cria novo produto com dados fiscais do XML.
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
        return product;
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

    private Payable createPayable(NfeXmlParser.ParsedNfe nfe, Long supplierId, String invoiceNumber) {
        Payable payable = new Payable();
        payable.setDescription(nfe.payableDescription());
        payable.setValue(nfe.totalValue());
        payable.setPaidAmount(BigDecimal.ZERO);
        payable.setIssueDate(nfe.issueDate());
        payable.setSupplierId(supplierId);
        payable.setSourceType(PayableSource.PURCHASE_INVOICE);
        payable.setPurchaseInvoiceNumber(invoiceNumber);
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

    private String readXml(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new NfeImportException("Arquivo XML vazio ou não enviado.");
        }
        try {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
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