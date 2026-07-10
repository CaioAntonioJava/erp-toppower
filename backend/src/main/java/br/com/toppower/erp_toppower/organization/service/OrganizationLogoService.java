package br.com.toppower.erp_toppower.organization.service;

import br.com.toppower.erp_toppower.config.AppProperties;
import br.com.toppower.erp_toppower.organization.entity.Organization;
import br.com.toppower.erp_toppower.organization.exception.InvalidLogoException;
import br.com.toppower.erp_toppower.organization.exception.OrganizationNotFoundException;
import br.com.toppower.erp_toppower.organization.repository.OrganizationRepository;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Serviço responsável pelo upload/remoção do logo de uma Organization.
 *
 * <p>O arquivo é salvo em {@code <app.uploads.dir>/logos/<uuid>.<ext>}
 * e a URL pública (servida pelo {@code WebMvcConfig}) é persistida no
 * campo {@code logoUrl} da Organization. Apenas ADMIN pode mexer
 * (controlado no controller).</p>
 *
 * <p>O nome do arquivo no disco é derivado do UUID da Organization
 * (e não do nome original enviado), o que evita:</p>
 * <ul>
 *   <li>colisões por nomes iguais;</li>
 *   <li>caracteres perigosos no path (../../, espaços, etc.);</li>
 *   <li>sobra de arquivos antigos (re-upload sobrescreve o anterior
 *       deterministicamente).</li>
 * </ul>
 */
@Service
public class OrganizationLogoService {

    /**
     * Extensões aceitas para upload de logo. SVG intencionalmente
     * ausente: o OpenHTMLtoPDF (renderer HTML→PDF usado nos PDFs
     * de cotação / pedido / proposta técnica) não suporta SVG —
     * Java ImageIO não tem decoder e o renderer lança
     * {@code IOException: Unrecognized Image format}.
     */
    private static final List<String> ALLOWED_EXTENSIONS = List.of("png", "jpg", "jpeg");

    private final OrganizationRepository organizationRepository;
    private final AppProperties appProperties;

    public OrganizationLogoService(OrganizationRepository organizationRepository,
                                  AppProperties appProperties) {
        this.organizationRepository = organizationRepository;
        this.appProperties = appProperties;
    }

    /**
     * Faz upload do logo, salvando no disco e atualizando o campo
     * {@code logoUrl} da Organization. Retorna a Organization atualizada.
     */
    @Transactional
    public Organization uploadLogo(UUID organizationId, MultipartFile file) {
        validate(file);

        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));

        String extension = resolveExtension(file);
        Path targetDir = logosDirectory();
        Path targetFile = targetDir.resolve(org.getUuid() + "." + extension);

        try (InputStream in = file.getInputStream()) {
            Files.createDirectories(targetDir);
            Files.copy(in, targetFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new InvalidLogoException(
                    "Falha ao gravar o arquivo do logo: " + ex.getMessage());
        }

        String publicUrl = "/logos/" + org.getUuid() + "." + extension;
        org.setLogoUrl(publicUrl);
        Organization saved = organizationRepository.save(org);

        // Limpa versões anteriores (mesmo UUID mas extensão diferente —
        // ex.: admin trocou PNG por SVG). Best-effort: falha não aborta
        // o upload.
        cleanupOtherExtensions(targetDir, org.getUuid().toString(), extension);

        return saved;
    }

    /**
     * Remove o logo (arquivo + campo {@code logoUrl}). No-op se a
     * Organization não tem logo configurado.
     */
    @Transactional
    public Organization deleteLogo(UUID organizationId) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));

        if (org.getLogoUrl() == null || org.getLogoUrl().isBlank()) {
            return org;
        }

        // /logos/<uuid>.<ext> → <uploads.dir>/logos/<uuid>.<ext>
        String filename = org.getLogoUrl().substring("/logos/".length());
        Path target = logosDirectory().resolve(filename);
        try {
            Files.deleteIfExists(target);
        } catch (IOException ex) {
            // Log silencioso: o campo logoUrl será zerado mesmo se o
            // arquivo não pôde ser removido — o admin pode limpar manualmente.
        }
        org.setLogoUrl(null);
        return organizationRepository.save(org);
    }

    // -----------------------------------------------------------------
    // Helpers privados
    // -----------------------------------------------------------------

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidLogoException("Arquivo de logo vazio ou não enviado.");
        }
        String contentType = file.getContentType();
        List<String> allowed = appProperties.getUploads().getLogo().getAllowedContentTypes();
        if (contentType == null || !allowed.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new InvalidLogoException(
                    "Tipo de arquivo não permitido. Aceitos: " + String.join(", ", allowed));
        }
        String extension = resolveExtension(file);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new InvalidLogoException(
                    "Extensão não permitida. Aceitas: " + String.join(", ", ALLOWED_EXTENSIONS));
        }
    }

    /**
     * Extrai a extensão do nome original do arquivo, normalizada em
     * minúsculas. Para {@code image/jpeg}, tanto {@code jpg} quanto
     * {@code jpeg} são aceitos. SVG é rejeitado (ver
     * {@link #ALLOWED_EXTENSIONS}).
     */
    private String resolveExtension(MultipartFile file) {
        String original = file.getOriginalFilename();
        String ext = (original != null)
                ? FilenameUtils.getExtension(original).toLowerCase(Locale.ROOT)
                : "";
        if (ext.isEmpty()) {
            // Fallback pelo content type
            ext = switch (file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT)) {
                case "image/png" -> "png";
                case "image/jpeg" -> "jpg";
                default -> "";
            };
        }
        return ext;
    }

    private Path logosDirectory() {
        return Paths.get(appProperties.getUploadsDir(), "logos").toAbsolutePath().normalize();
    }

    /**
     * Remove versões antigas do logo (mesmo UUID, extensão diferente)
     * após um novo upload. Evita acúmulo de lixo no disco.
     */
    private void cleanupOtherExtensions(Path dir, String uuidPrefix, String keepExtension) {
        try (var stream = Files.list(dir)) {
            stream.filter(p -> {
                String name = p.getFileName().toString();
                return name.startsWith(uuidPrefix + ".")
                        && !name.endsWith("." + keepExtension);
            }).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // best-effort
                }
            });
        } catch (IOException ignored) {
            // best-effort
        }
    }
}