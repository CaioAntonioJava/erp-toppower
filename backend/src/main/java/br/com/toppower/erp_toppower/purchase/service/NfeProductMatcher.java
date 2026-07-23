package br.com.toppower.erp_toppower.purchase.service;

import br.com.toppower.erp_toppower.product.entity.Product;
import br.com.toppower.erp_toppower.product.enums.ProductStatus;
import br.com.toppower.erp_toppower.product.repository.ProductRepository;
import br.com.toppower.erp_toppower.purchase.dto.ItemStatus;
import br.com.toppower.erp_toppower.purchase.entity.ProductSupplierCode;
import br.com.toppower.erp_toppower.purchase.repository.ProductSupplierCodeRepository;
import br.com.toppower.erp_toppower.purchase.util.SimilarityUtil;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Responsável por classificar cada item da NF-e durante o preview,
 * aplicando a estratégia de matching em ordem de prioridade:
 *
 * <ol>
 *   <li><b>Código do fornecedor</b> — {@code product_supplier_codes}
 *       por (supplierId, cProd). Match mais forte, pois é a relação
 *       explícita construída em importações anteriores.</li>
 *   <li><b>Código EAN/GTIN</b> — {@code Product.codigoBarras}.</li>
 *   <li><b>Código interno (SKU)</b> — {@code Product.code}.</li>
 *   <li><b>Similaridade de nome</b> — Levenshtein sobre produtos
 *       candidatos (pré-filtrados por NCM quando disponível).
 *       Acima do limiar vira {@link ItemStatus#DIVERGENTE} para o
 *       usuário decidir.</li>
 *   <li><b>Nenhum match</b> — {@link ItemStatus#NOVO}.</li>
 * </ol>
 *
 * <p>Os três primeiros critérios resultam em {@link ItemStatus#EXISTENTE}
 * (match determinístico). A similaridade de nome resulta em
 * {@link ItemStatus#DIVERGENTE} (match sugerido, não automático).</p>
 */
@Component
public class NfeProductMatcher {

    /** Limiar de similaridade (0–1) para sugerir vínculo por nome. */
    static final double NAME_SIMILARITY_THRESHOLD = 0.60;

    private final ProductRepository productRepository;
    private final ProductSupplierCodeRepository productSupplierCodeRepository;

    public NfeProductMatcher(ProductRepository productRepository,
                            ProductSupplierCodeRepository productSupplierCodeRepository) {
        this.productRepository = productRepository;
        this.productSupplierCodeRepository = productSupplierCodeRepository;
    }

    /**
     * Resultado do matching de um item.
     *
     * @param status        status classificado.
     * @param productId     ID do produto (em EXISTENTE/DIVERGENTE).
     * @param matchReason   motivo do match (FORNECEDOR/EAN/CODIGO/NOME) ou null.
     * @param existingProductName nome do produto cadastrado (para comparação).
     */
    public record MatchResult(
            ItemStatus status,
            Long productId,
            String matchReason,
            String existingProductName
    ) {
        static MatchResult novo() {
            return new MatchResult(ItemStatus.NOVO, null, null, null);
        }

        static MatchResult existente(Product p, String reason) {
            return new MatchResult(ItemStatus.EXISTENTE, p.getId(), reason, p.getName());
        }

        static MatchResult divergente(Product p, String reason) {
            return new MatchResult(ItemStatus.DIVERGENTE, p.getId(), reason, p.getName());
        }
    }

    /**
     * Classifica um item da NF-e contra o catálogo da organização.
     *
     * @param supplierId ID do fornecedor (emitente) já resolvido, ou
     *                   null quando o fornecedor ainda não está cadastrado.
     * @param code       cProd da NF-e (código no fornecedor).
     * @param codigoBarras cEAN/GTIN da NF-e.
     * @param name       xProd (descrição) da NF-e.
     * @param ncm        NCM da NF-e (para pré-filtro da similaridade).
     */
    public MatchResult match(Long supplierId, String code, String codigoBarras,
                              String name, String ncm) {
        // 1. Código do fornecedor (relação persistida).
        if (supplierId != null && code != null && !code.isBlank()) {
            Optional<ProductSupplierCode> rel =
                    productSupplierCodeRepository.findBySupplierIdAndSupplierCode(supplierId, code.trim());
            if (rel.isPresent()) {
                Optional<Product> p = productRepository.findById(rel.get().getProductId());
                if (p.isPresent()) {
                    return MatchResult.existente(p.get(), "FORNECEDOR");
                }
            }
        }

        // 2. Código EAN/GTIN.
        if (codigoBarras != null && !codigoBarras.isBlank()) {
            Optional<Product> byEan = productRepository.findByCodigoBarras(codigoBarras.trim());
            if (byEan.isPresent()) {
                return MatchResult.existente(byEan.get(), "EAN");
            }
        }

        // 3. Código interno (SKU).
        if (code != null && !code.isBlank()) {
            Optional<Product> byCode = productRepository.findByCode(code.trim());
            if (byCode.isPresent()) {
                return MatchResult.existente(byCode.get(), "CODIGO");
            }
        }

        // 4. Similaridade de nome (fuzzy) — pré-filtrado por NCM.
        Optional<Product> byName = findByNameSimilarity(name, ncm);
        if (byName.isPresent()) {
            return MatchResult.divergente(byName.get(), "NOME");
        }

        // 5. Nenhum match.
        return MatchResult.novo();
    }

    /**
     * Busca o produto ativo mais similar por nome. Pré-filtra por NCM
     * (quando disponível) para reduzir o universo e falsos positivos.
     * Retorna o primeiro acima do limiar.
     */
    private Optional<Product> findByNameSimilarity(String name, String ncm) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        List<Product> candidates;
        if (ncm != null && !ncm.isBlank()) {
            candidates = productRepository.findByStatusAndNcm(ProductStatus.ATIVO, ncm.trim());
        } else {
            candidates = productRepository.findByStatus(ProductStatus.ATIVO);
        }
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        Product best = null;
        double bestScore = 0.0;
        for (Product p : candidates) {
            if (p.getName() == null || p.getName().isBlank()) {
                continue;
            }
            double score = SimilarityUtil.similarity(name, p.getName());
            if (score > bestScore) {
                bestScore = score;
                best = p;
            }
        }
        if (best != null && bestScore >= NAME_SIMILARITY_THRESHOLD) {
            return Optional.of(best);
        }
        return Optional.empty();
    }
}