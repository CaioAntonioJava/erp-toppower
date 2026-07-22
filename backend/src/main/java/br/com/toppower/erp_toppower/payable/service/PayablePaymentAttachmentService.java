package br.com.toppower.erp_toppower.payable.service;

import br.com.toppower.erp_toppower.config.AppProperties;
import br.com.toppower.erp_toppower.payable.entity.PayablePayment;
import br.com.toppower.erp_toppower.payable.exception.PayableBusinessException;
import br.com.toppower.erp_toppower.payable.repository.PayablePaymentRepository;
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
 * Serviço responsável por salvar/carregar/remover comprovantes de
 * pagamento (receipts) anexados a pagamentos de contas a pagar.
 *
 * <p>Os arquivos são gravados em disco sob
 * {@code <app.uploads.dir>/attachments/payments/} e a URL autenticada
 * é armazenada no campo {@link PayablePayment#getReceiptUrl()}.</p>
 */
@Service
public class PayablePaymentAttachmentService {

    private static final Logger log = LoggerFactory.getLogger(PayablePaymentAttachmentService.class);

    private static final List<String> ALLOWED_EXTENSIONS = List.of("pdf", "png", "jpg", "jpeg");
    private static final long MAX_SIZE_BYTES = 10L * 1024L * 1024L; // 10MB

    private final PayablePaymentRepository paymentRepository;
    private final AppProperties appProperties;

    public PayablePaymentAttachmentService(PayablePaymentRepository paymentRepository,
                                           AppProperties appProperties) {
        this.paymentRepository = paymentRepository;
        this.appProperties = appProperties;
    }

    private Path receiptsDirectory() {
        return Paths.get(appProperties.getUploadsDir(), "attachments", "payments")
                .toAbsolutePath().normalize();
    }

    /**
     * Salva um comprovante de pagamento em disco e atualiza o campo
     * {@code receiptUrl} no pagamento.
     *
     * @param paymentId ID do pagamento
     * @param file      arquivo enviado (PDF, PNG, JPEG)
     * @return URL autenticada do comprovante
     */
    @Transactional
    public String save(Long paymentId, MultipartFile file) {
        validate(file);

        PayablePayment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PayableBusinessException(
                        "Pagamento não encontrado: " + paymentId));

        String originalName = file.getOriginalFilename();
        String extension = FilenameUtils.getExtension(originalName);
        String storedName = "receipt-" + paymentId + "-" + UUID.randomUUID() + "." + extension;

        try {
            Path targetDir = receiptsDirectory();
            Files.createDirectories(targetDir);
            Path targetFile = targetDir.resolve(storedName).normalize();
            if (!targetFile.startsWith(targetDir)) {
                throw new PayableBusinessException("Nome de arquivo inválido.");
            }
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            log.error("Falha ao gravar comprovante do pagamento {}: {}", paymentId, ex.getMessage(), ex);
            throw new PayableBusinessException("Falha ao gravar o arquivo. Tente novamente.");
        }

        String receiptUrl = "/api/v1/accounts-payable/payments/" + paymentId + "/receipt";
        payment.setReceiptUrl(receiptUrl);
        paymentRepository.save(payment);
        return receiptUrl;
    }

    /**
     * Carrega os bytes do comprovante de pagamento para download/exibição.
     */
    @Transactional(readOnly = true)
    public LoadedFile loadFile(Long paymentId) {
        PayablePayment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PayableBusinessException(
                        "Pagamento não encontrado: " + paymentId));
        if (payment.getReceiptUrl() == null) {
            throw new PayableBusinessException(
                    "Pagamento " + paymentId + " não possui comprovante.");
        }
        // O storedName é derivado do paymentId — busca o arquivo no disco.
        try {
            Path targetDir = receiptsDirectory();
            // Procura qualquer arquivo que comece com "receipt-" + paymentId + "-"
            try (var files = Files.list(targetDir)) {
                var match = files
                        .filter(f -> f.getFileName().toString().startsWith("receipt-" + paymentId + "-"))
                        .findFirst();
                if (match.isEmpty()) {
                    throw new PayableBusinessException("Arquivo de comprovante não encontrado.");
                }
                Path targetFile = match.get().normalize();
                if (!targetFile.startsWith(targetDir)) {
                    throw new PayableBusinessException("Arquivo inválido.");
                }
                byte[] bytes = Files.readAllBytes(targetFile);
                String contentType = detectContentType(targetFile);
                return new LoadedFile(targetFile.getFileName().toString(), contentType, bytes);
            }
        } catch (IOException ex) {
            log.error("Falha ao ler comprovante do pagamento {}: {}", paymentId, ex.getMessage(), ex);
            throw new PayableBusinessException("Falha ao ler o arquivo.");
        }
    }

    /**
     * Remove o comprovante de pagamento (arquivo em disco + referência).
     */
    @Transactional
    public void delete(Long paymentId) {
        PayablePayment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PayableBusinessException(
                        "Pagamento não encontrado: " + paymentId));
        if (payment.getReceiptUrl() == null) {
            return;
        }
        try {
            Path targetDir = receiptsDirectory();
            try (var files = Files.list(targetDir)) {
                var match = files
                        .filter(f -> f.getFileName().toString().startsWith("receipt-" + paymentId + "-"))
                        .findFirst();
                if (match.isPresent()) {
                    Path targetFile = match.get().normalize();
                    if (targetFile.startsWith(targetDir)) {
                        Files.deleteIfExists(targetFile);
                    }
                }
            }
        } catch (IOException ex) {
            log.warn("Falha ao remover comprovante do pagamento {}: {}", paymentId, ex.getMessage());
        }
        payment.setReceiptUrl(null);
        paymentRepository.save(payment);
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new PayableBusinessException("Arquivo vazio não é permitido.");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new PayableBusinessException(
                    "Tamanho máximo permitido é 10MB. Recebido: " + file.getSize() + " bytes.");
        }
        String contentType = file.getContentType();
        List<String> allowed = appProperties.getUploads().getAttachment().getAllowedContentTypes();
        if (contentType == null || !allowed.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new PayableBusinessException(
                    "Tipo de arquivo não permitido: " + contentType
                            + ". Aceitos: " + String.join(", ", allowed));
        }
        String originalName = file.getOriginalFilename();
        String extension = FilenameUtils.getExtension(originalName);
        if (extension == null
                || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT))) {
            throw new PayableBusinessException(
                    "Extensão não permitida. Aceitas: " + String.join(", ", ALLOWED_EXTENSIONS));
        }
    }

    private String detectContentType(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".pdf")) return "application/pdf";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        return "application/octet-stream";
    }

    public record LoadedFile(String fileName, String contentType, byte[] bytes) {
    }
}
