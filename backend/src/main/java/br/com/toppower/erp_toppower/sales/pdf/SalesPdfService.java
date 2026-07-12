package br.com.toppower.erp_toppower.sales.pdf;

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.PageSizeUnits;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.util.Locale;
import java.util.Map;

/**
 * Renderiza templates Thymeleaf em PDF via OpenHTMLtoPDF.
 *
 * <p>Pipeline:</p>
 * <ol>
 *   <li>Thymeleaf processa o HTML com o modelo (issuer + dados do
 *       documento) → string XHTML estrito.</li>
 *   <li>OpenHTMLtoPDF ({@link PdfRendererBuilder}) parseia o XHTML e
 *       gera o PDF em memória ({@code ByteArrayOutputStream}) usando
 *       Apache PDFBox 2.x como motor de saída.</li>
 * </ol>
 *
 * <p>Configurações importantes:</p>
 * <ul>
 *   <li>{@code @page} CSS: definido em cada template, tamanho A4,
 *       margem controlada. {@code -fs-table-paginate: paginate}
 *       repete cabeçalhos de tabela entre páginas (feature do
 *       OpenHTMLtoPDF, ausente no Flying Saucer).</li>
 *   <li>{@code useFastMode()}: modo de renderização rápido,
 *       recomendado em produção desde 1.0.5.</li>
 *   <li>{@code useDefaultPageSize(210, 297, MM)}: fallback A4 se o
 *       CSS do template não declarar {@code @page size}.</li>
 *   <li>{@code withHtmlContent(xhtml, null)}: passamos
 *       {@code baseUri=null} porque o logo é embutido como data URI
 *       Base64 (ver {@link ImageEmbedder}) — não há imagens
 *       referenciadas por URL relativa.</li>
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

        // OpenHTMLtoPDF usa um parser XML estrito:
        //  (1) rejeita tags HTML não-fechadas como <br> (exige <br/>).
        //      Conteúdo dinâmico vindo de th:utext (observações,
        //      descrições, cláusulas) pode conter <br> sem barra final,
        //      então normalizamos antes de enviar ao renderizador.
        //  (2) não declara entidades HTML nomeadas como &nbsp; — apenas
        //      as 5 entidades XML padrão (&amp;, &lt;, &gt;, &quot;,
        //      &apos;). Conteúdo rich text (editores WYSIWYG) costuma
        //      serializar espaços não-quebráveis como &nbsp;, o que
        //      estoura o SAXParser com "The entity 'nbsp' was
        //      referenced, but not declared". Substituímos pelas formas
        //      numéricas (&#160;) que o XML aceita sem precisar de DTD.
        xhtml = xhtml.replaceAll("(?i)<br(\\s[^>]*)?>", "<br$1/>");
        xhtml = xhtml
                .replace("&nbsp;", "&#160;")
                .replace("&copy;", "&#169;")
                .replace("&reg;", "&#174;")
                .replace("&trade;", "&#8482;")
                .replace("&mdash;", "&#8212;")
                .replace("&ndash;", "&#8211;")
                .replace("&hellip;", "&#8230;")
                .replace("&laquo;", "&#171;")
                .replace("&raquo;", "&#187;")
                .replace("&lsquo;", "&#8216;")
                .replace("&rsquo;", "&#8217;")
                .replace("&ldquo;", "&#8220;")
                .replace("&rdquo;", "&#8221;")
                .replace("&bull;", "&#8226;")
                .replace("&middot;", "&#183;");

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            // A4 retrato como fallback quando o CSS do template não
            // declara @page size. O CSS do styles.html já define A4,
            // então esta linha só atua em caso de template sem @page.
            builder.useDefaultPageSize(210f, 297f, PageSizeUnits.MM);
            // baseUri null: imagens vêm como data URI (ImageEmbedder).
            builder.withHtmlContent(xhtml, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception ex) {
            // Exception (não RuntimeException) porque o OpenHTMLtoPDF
            // pode lançar checked exceptions (DocumentException, IOException).
            throw new PdfGenerationException(
                    "Falha ao gerar PDF a partir do template '"
                            + templateName + "': " + ex.getMessage(), ex);
        }
    }
}
