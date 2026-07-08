package br.com.toppower.erp_toppower.sales.pdf;

import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.util.Locale;
import java.util.Map;

/**
 * Renderiza templates Thymeleaf em PDF via Flying Saucer.
 *
 * <p>Pipeline:</p>
 * <ol>
 *   <li>Thymeleaf processa o HTML com o modelo (issuer + dados do
 *       documento) → string XHTML estrito.</li>
 *   <li>Flying Saucer (ITextRenderer) parseia o XHTML e gera o PDF
 *       em memória ({@code ByteArrayOutputStream}).</li>
 * </ol>
 *
 * <p>Configurações importantes:</p>
 * <ul>
 *   <li>{@code @page} CSS: definido em cada template, tamanho A4,
 *       margem controlada;</li>
 *   <li>{@code SharedContext}: idioma pt-BR para quebras de linha
 *       corretas em nomes longos.</li>
 * </ul>
 *
 * <p>Esta classe NÃO conhece cada tipo de documento — ela apenas
 * recebe um nome de template e o modelo pronto. As classes de
 * documento (Cotação, Proposta, Pedido) decidem o que vai no modelo.</p>
 */
@Service
public class SalesPdfService {

    private final TemplateEngine templateEngine;

    public SalesPdfService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    /**
     * Renderiza o template Thymeleaf (referente ao nome lógico
     * {@code templateName}, resolvido pelo resolver configurado pelo
     * {@code spring-boot-starter-thymeleaf} — por default
     * {@code classpath:/templates/<name>.html}) com o modelo fornecido.
     *
     * @param templateName nome do template (ex.: {@code "pdf/quotation"})
     * @param model        dados a serem interpolados pelo Thymeleaf
     * @return bytes do PDF gerado
     */
    public byte[] render(String templateName, Map<String, Object> model) {
        Context thymeleafContext = new Context(Locale.forLanguageTag("pt-BR"), model);

        String xhtml;
        try {
            xhtml = templateEngine.process(templateName, thymeleafContext);
        } catch (RuntimeException ex) {
            // Repassa como PdfGenerationException (500) em vez de deixar
            // o IllegalStateException genérico virar 400 enganoso ("selecione
            // uma empresa ativa") no GlobalExceptionHandler.
            throw new PdfGenerationException(
                    "Falha ao renderizar template Thymeleaf '" + templateName
                            + "': " + ex.getMessage(), ex);
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(xhtml);
            renderer.layout();
            renderer.createPDF(out);
            renderer.finishPDF();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new PdfGenerationException(
                    "Falha ao gerar PDF a partir do template '"
                            + templateName + "': " + ex.getMessage(), ex);
        }
    }
}