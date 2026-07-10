package br.com.toppower.erp_toppower.sales.pdf;

import br.com.toppower.erp_toppower.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Locale;

/**
 * Converte URLs/imagens locais em <strong>data URIs Base64</strong>
 * para serem embutidas no XHTML que o Flying Saucer renderiza.
 *
 * <h2>Por que isso existe</h2>
 *
 * <p>O OpenHTMLtoPDF (renderer HTML→PDF usado pelo backend) tem uma
 * limitação conhecida: quando o documento é carregado via
 * {@code PdfRendererBuilder.withHtmlContent(xhtml, null)} sem um
 * {@code baseUri} apontando para o classpath/sistema de arquivos,
 * resolver {@code <img src="/logos/uuid.png">} falha silenciosamente
 * — a imagem simplesmente não aparece no PDF, sem nenhum erro
 * visível. A mesma limitação existia no Flying Saucer
 * {@code ITextRenderer.setDocumentFromString(xhtml)}.</p>
 *
 * <p>A solução padrão é embutir a imagem como {@code data:image/png;base64,...}
 * direto no atributo {@code src}. Browsers e o OpenHTMLtoPDF
 * interpretam data URIs nativamente; o custo é um PDF um pouco maior
 * (alguns KB), que para logos não é problema.</p>
 *
 * <h2>Como funciona</h2>
 *
 * <ol>
 *   <li>Recebe uma URL pública (ex.: {@code /logos/<uuid>.png});</li>
 *   <li>Mapeia para o arquivo local em
 *       {@code <app.uploads.dir>/logos/};</li>
 *   <li>Lê os bytes, codifica em Base64 e devolve o data URI
 *       correspondente (com prefixo de content-type).</li>
 * </ol>
 *
 * <p>Falhas (arquivo ausente, permissão negada, content-type
 * desconhecido) são logadas em WARN e resultam em {@code null} — o
 * caller decide o fallback (logo padrão, texto, etc.).</p>
 */
@Component
public class ImageEmbedder {

    private static final Logger log = LoggerFactory.getLogger(ImageEmbedder.class);

    /**
     * Content-types suportados pelo OpenHTMLtoPDF.
     *
     * <p>O renderer resolve imagens via {@link javax.imageio.ImageIO},
     * que aceita apenas formatos raster (PNG, JPEG, BMP, GIF, WBMP).
     * <strong>SVG não é suportado</strong> — uploads SVG seriam
     * silenciosamente ignorados pelo renderer e gerariam
     * {@code IOException: Unrecognized Image format}. Por isso
     * SVGs são rejeitados aqui (retornam {@code null}) e o upload de
     * logo também bloqueia SVG na camada de validação.</p>
     */
    private static final String DATA_PREFIX_PNG = "data:image/png;base64,";
    private static final String DATA_PREFIX_JPG = "data:image/jpeg;base64,";

    private final AppProperties appProperties;

    public ImageEmbedder(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    /**
     * Converte uma URL pública {@code /logos/<arquivo>} em data URI Base64.
     *
     * @param publicUrl URL relativa retornada pela API (ex.: {@code /logos/abc.svg})
     * @return data URI pronto para uso em {@code <img src="...">}, ou
     *         {@code null} se a URL for inválida/ausente ou o arquivo não
     *         puder ser lido.
     */
    public String toDataUri(String publicUrl) {
        if (publicUrl == null || publicUrl.isBlank()) {
            return null;
        }
        if (!publicUrl.startsWith("/logos/")) {
            log.warn("toDataUri: URL fora do escopo esperado (/logos/**): {}", publicUrl);
            return null;
        }

        String filename = publicUrl.substring("/logos/".length());
        // Bloqueia tentativas de path traversal: nome deve ser simples
        // (sem '/', sem '..').
        if (filename.contains("/") || filename.contains("..") || filename.isBlank()) {
            log.warn("toDataUri: nome de arquivo inválido: {}", filename);
            return null;
        }

        Path file = logosDir().resolve(filename).normalize();
        // Garante que o arquivo resolvido ainda está dentro do diretório
        // de logos (defesa em profundidade contra path traversal via symlink).
        if (!file.startsWith(logosDir())) {
            log.warn("toDataUri: tentativa de escape do diretório de logos: {}", file);
            return null;
        }

        if (!Files.exists(file)) {
            log.warn("toDataUri: arquivo de logo não encontrado: {}", file);
            return null;
        }

        byte[] bytes;
        try {
            bytes = Files.readAllBytes(file);
        } catch (IOException ex) {
            log.warn("toDataUri: falha ao ler {}: {}", file, ex.getMessage());
            return null;
        }

        String extension = filename.substring(filename.lastIndexOf('.') + 1)
                .toLowerCase(Locale.ROOT);
        String prefix = switch (extension) {
            case "png" -> DATA_PREFIX_PNG;
            case "jpg", "jpeg" -> DATA_PREFIX_JPG;
            // SVG intencionalmente NÃO mapeado: OpenHTMLtoPDF não
            // suporta SVG (Java ImageIO não tem decoder). Embed seria
            // ignorado pelo renderer e geraria IOException.
            default -> {
                log.warn("toDataUri: extensão não suportada para embed: {}", extension);
                yield null;
            }
        };
        if (prefix == null) return null;

        String base64 = Base64.getEncoder().encodeToString(bytes);
        return prefix + base64;
    }

    private Path logosDir() {
        return Paths.get(appProperties.getUploadsDir(), "logos").toAbsolutePath().normalize();
    }

    /**
     * Stub mantido para uso futuro caso seja necessário carregar uma imagem
     * a partir de uma URL HTTP (não usado hoje — todos os logos vivem no
     * disco local).
     */
    @SuppressWarnings("unused")
    private byte[] readRemote(String url) throws IOException {
        try (InputStream in = new URI(url).toURL().openStream()) {
            return in.readAllBytes();
        } catch (URISyntaxException ex) {
            throw new IOException("URL inválida: " + url, ex);
        }
    }
}