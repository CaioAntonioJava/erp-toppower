package br.com.toppower.erp_toppower.sales.pdf;

import br.com.toppower.erp_toppower.config.AppProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes de unidade do {@link ImageEmbedder}.
 *
 * <p>Estratégia: configurar {@code AppProperties.uploadsDir} para um
 * diretório temporário isolado por teste, gravar arquivos sintéticos
 * (PNG, JPEG, e um SVG para confirmar a rejeição) e validar o
 * data URI retornado (prefixo correto) ou a rejeição (null).</p>
 */
class ImageEmbedderTest {

    private Path tempDir;
    private ImageEmbedder embedder;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("img-embedder-test-");
        AppProperties props = new AppProperties();
        props.setUploadsDir(tempDir.toString());
        // Cria o subdiretório logos/ que o serviço espera.
        Files.createDirectories(tempDir.resolve("logos"));
        embedder = new ImageEmbedder(props);
    }

    @AfterEach
    void tearDown() throws IOException {
        // Limpa o diretório temporário
        if (tempDir != null && Files.exists(tempDir)) {
            Files.walk(tempDir)
                    .sorted((a, b) -> b.getNameCount() - a.getNameCount())
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                    });
        }
    }

    @Test
    void toDataUri_withSvgFile_returnsNull() throws IOException {
        // SVG não é suportado pelo OpenHTMLtoPDF (Java ImageIO não tem
        // decoder). Mesmo que o arquivo esteja no disco, toDataUri deve
        // retornar null em vez de embutir um data URI que o renderer
        // não saberia decodificar.
        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 100 100\"><rect width=\"100\" height=\"100\" fill=\"red\"/></svg>";
        Files.write(tempDir.resolve("logos").resolve("test-org.svg"), svg.getBytes());

        assertNull(embedder.toDataUri("/logos/test-org.svg"));
    }

    @Test
    void toDataUri_withPngFile_returnsPngDataUri() throws IOException {
        // Bytes "mágicos" do PNG: 89 50 4E 47 0D 0A 1A 0A + filler
        byte[] pngHeader = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00};
        Files.write(tempDir.resolve("logos").resolve("logo.png"), pngHeader);

        String dataUri = embedder.toDataUri("/logos/logo.png");

        assertNotNull(dataUri);
        assertTrue(dataUri.startsWith("data:image/png;base64,"), "Prefixo errado: " + dataUri.substring(0, Math.min(40, dataUri.length())));
    }

    @Test
    void toDataUri_withJpgFile_returnsJpegDataUri() throws IOException {
        // Bytes "mágicos" do JPEG: FF D8 FF
        byte[] jpgHeader = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};
        Files.write(tempDir.resolve("logos").resolve("logo.jpg"), jpgHeader);

        String dataUri = embedder.toDataUri("/logos/logo.jpg");

        assertNotNull(dataUri);
        assertTrue(dataUri.startsWith("data:image/jpeg;base64,"), "Prefixo errado");
    }

    @Test
    void toDataUri_withNullUrl_returnsNull() {
        assertNull(embedder.toDataUri(null));
    }

    @Test
    void toDataUri_withEmptyUrl_returnsNull() {
        assertNull(embedder.toDataUri(""));
        assertNull(embedder.toDataUri("   "));
    }

    @Test
    void toDataUri_withNonLogoUrl_returnsNull() {
        // URL fora do escopo /logos/** deve ser rejeitada.
        assertNull(embedder.toDataUri("/api/v1/anything/else.png"));
        assertNull(embedder.toDataUri("https://example.com/logo.png"));
    }

    @Test
    void toDataUri_withMissingFile_returnsNull() {
        // Arquivo não existe no disco → null (não lança exception).
        assertNull(embedder.toDataUri("/logos/nao-existe.svg"));
    }

    @Test
    void toDataUri_withPathTraversal_returnsNull() {
        // Tentativas de escape do diretório devem ser bloqueadas.
        assertNull(embedder.toDataUri("/logos/../../../etc/passwd"));
        assertNull(embedder.toDataUri("/logos/sub/dir/file.png"));
    }

    @Test
    void toDataUri_withUnsupportedExtension_returnsNull() throws IOException {
        Files.write(tempDir.resolve("logos").resolve("logo.bmp"), new byte[]{0x00});
        assertNull(embedder.toDataUri("/logos/logo.bmp"));
    }
}