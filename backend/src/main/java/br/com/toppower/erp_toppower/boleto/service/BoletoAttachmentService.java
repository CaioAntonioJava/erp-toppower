package br.com.toppower.erp_toppower.boleto.service;

import br.com.toppower.erp_toppower.boleto.dto.BoletoAttachmentResponse;
import br.com.toppower.erp_toppower.boleto.entity.Boleto;
import br.com.toppower.erp_toppower.boleto.entity.BoletoAttachment;
import br.com.toppower.erp_toppower.boleto.exception.BoletoAttachmentNotFoundException;
import br.com.toppower.erp_toppower.boleto.exception.BoletoNotFoundException;
import br.com.toppower.erp_toppower.boleto.exception.InvalidBoletoAttachmentException;
import br.com.toppower.erp_toppower.boleto.mapper.BoletoAttachmentMapper;
import br.com.toppower.erp_toppower.boleto.repository.BoletoAttachmentRepository;
import br.com.toppower.erp_toppower.boleto.repository.BoletoRepository;
import br.com.toppower.erp_toppower.common.enums.RegistrationStatus;
import br.com.toppower.erp_toppower.config.AppProperties;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * Serviço responsável pelo upload/listagem/download/remoção de anexos
 * de boletos (PDF/PNG/JPEG).
 *
 * <p>Espelha {@code OrganizationLogoService}: valida content-type/extensão,
 * grava o arquivo em disco sob {@code <app.uploads.dir>/attachments/boletos/}
 * e persiste apenas metadados + URL (autenticada) no banco. O acesso ao
 * arquivo sempre passa pelo endpoint autenticado (preserva o escopo
 * multi-tenant), diferentemente dos logos que são servidos publicamente.</p>
 */
@Service
public class BoletoAttachmentService {

    private static final Logger log = LoggerFactory.getLogger(BoletoAttachmentService.class);

    /** Extensões aceitas para anexo de boleto (PDF + imagens). */
    private static final List<String> ALLOWED_EXTENSIONS = List.of("pdf", "png", "jpg", "jpeg");

    private static final long MAX_SIZE_BYTES = 10L * 1024L * 1024L; // 10MB

    private final BoletoAttachmentRepository attachmentRepository;
    private final BoletoRepository boletoRepository;
    private final AppProperties appProperties;

    public BoletoAttachmentService(BoletoAttachmentRepository attachmentRepository,
                                    BoletoRepository boletoRepository,
                                    AppProperties appProperties) {
        this.attachmentRepository = attachmentRepository;
        this.boletoRepository = boletoRepository;
        this.appProperties = appProperties;
    }

    /** Diretório de armazenamento dos anexos de boletos. */
    private Path attachmentsDirectory() {
        return Paths.get(appProperties.getUploadsDir(), "attachments", "boletos")
                .toAbsolutePath().normalize();
    }

    /**
     * Anexa um arquivo a um boleto. Valida o boleto (existe + ATIVO) e o
     * arquivo (content-type, extensão, tamanho), grava no disco e persiste
     * os metadados.
     */
    @Transactional
    public BoletoAttachmentResponse upload(Long boletoId, MultipartFile file) {
        Boleto boleto = boletoRepository.findById(boletoId)
                .orElseThrow(() -> new BoletoNotFoundException(boletoId));
        if (boleto.getStatus() != RegistrationStatus.ATIVO) {
            throw new InvalidBoletoAttachmentException(
                    "Não é possível anexar a um boleto inativo: " + boletoId);
        }

        validate(file);

        String originalName = file.getOriginalFilename();
        String extension = FilenameUtils.getExtension(originalName);
        String storedName = boletoId + "-" + UUID.randomUUID() + "." + extension;

        try {
            Path targetDir = attachmentsDirectory();
            Files.createDirectories(targetDir);
            Path targetFile = targetDir.resolve(storedName).normalize();
            // Guarda contra path traversal no nome resolvido.
            if (!targetFile.startsWith(targetDir)) {
                throw new InvalidBoletoAttachmentException("Nome de arquivo inválido.");
            }
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            log.error("Falha ao gravar anexo do boleto {}: {}", boletoId, ex.getMessage(), ex);
            throw new InvalidBoletoAttachmentException(
                    "Falha ao gravar o arquivo. Tente novamente.");
        }

        BoletoAttachment attachment = new BoletoAttachment();
        attachment.setBoletoId(boletoId);
        attachment.setFileName(originalName);
        attachment.setStoredName(storedName);
        attachment.setContentType(file.getContentType());
        attachment.setSizeBytes(file.getSize());
        // Placeholder temporário — a URL real depende do id gerado.
        attachment.setPublicUrl("pending");
        BoletoAttachment saved = attachmentRepository.save(attachment);
        saved.setPublicUrl("/api/v1/boletos/" + boletoId + "/attachments/" + saved.getId() + "/file");
        BoletoAttachment finalSaved = attachmentRepository.save(saved);
        return BoletoAttachmentMapper.toResponse(finalSaved);
    }

    /** Valida content-type, extensão e tamanho do arquivo enviado. */
    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidBoletoAttachmentException("Arquivo vazio não é permitido.");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new InvalidBoletoAttachmentException(
                    "Tamanho máximo permitido é 10MB. Recebido: " + file.getSize() + " bytes.");
        }
        String contentType = file.getContentType();
        List<String> allowed = appProperties.getUploads().getAttachment().getAllowedContentTypes();
        if (contentType == null || !allowed.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new InvalidBoletoAttachmentException(
                    "Tipo de arquivo não permitido: " + contentType
                            + ". Aceitos: " + String.join(", ", allowed));
        }
        String originalName = file.getOriginalFilename();
        String extension = FilenameUtils.getExtension(originalName);
        if (extension == null
                || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT))) {
            throw new InvalidBoletoAttachmentException(
                    "Extensão não permitida. Aceitas: " + String.join(", ", ALLOWED_EXTENSIONS));
        }
    }

    /** Lista os anexos de um boleto. */
    @Transactional(readOnly = true)
    public List<BoletoAttachmentResponse> listByBoleto(Long boletoId) {
        return attachmentRepository.findByBoletoId(boletoId).stream()
                .map(BoletoAttachmentMapper::toResponse)
                .toList();
    }

    /** Carrega os bytes do anexo (para download/inline). Garante vínculo com o boleto. */
    @Transactional(readOnly = true)
    public LoadedFile loadFile(Long boletoId, Long attachmentId) {
        BoletoAttachment attachment = attachmentRepository.findByIdAndBoletoId(attachmentId, boletoId)
                .orElseThrow(() -> new BoletoAttachmentNotFoundException(boletoId, attachmentId));
        try {
            Path targetDir = attachmentsDirectory();
            Path targetFile = targetDir.resolve(attachment.getStoredName()).normalize();
            if (!targetFile.startsWith(targetDir) || !Files.exists(targetFile)) {
                throw new BoletoAttachmentNotFoundException(attachmentId);
            }
            byte[] bytes = Files.readAllBytes(targetFile);
            return new LoadedFile(attachment.getFileName(), attachment.getContentType(), bytes);
        } catch (IOException ex) {
            log.error("Falha ao ler anexo {}: {}", attachmentId, ex.getMessage(), ex);
            throw new InvalidBoletoAttachmentException("Falha ao ler o arquivo.");
        }
    }

    /** Remove um anexo (arquivo em disco + registro no banco). */
    @Transactional
    public void delete(Long boletoId, Long attachmentId) {
        BoletoAttachment attachment = attachmentRepository.findByIdAndBoletoId(attachmentId, boletoId)
                .orElseThrow(() -> new BoletoAttachmentNotFoundException(boletoId, attachmentId));
        try {
            Path targetDir = attachmentsDirectory();
            Path targetFile = targetDir.resolve(attachment.getStoredName()).normalize();
            if (targetFile.startsWith(targetDir)) {
                Files.deleteIfExists(targetFile);
            }
        } catch (IOException ex) {
            log.warn("Falha ao remover arquivo do anexo {}: {}", attachmentId, ex.getMessage());
        }
        attachmentRepository.delete(attachment);
    }

    /** Conteúdo carregado do disco para streaming ao cliente. */
    public record LoadedFile(String fileName, String contentType, byte[] bytes) {
    }
}