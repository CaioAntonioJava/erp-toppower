package br.com.toppower.erp_toppower.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configurações customizadas carregadas de {@code application.properties}
 * com prefixo {@code app.}.
 *
 * <p>Decidimos usar uma classe dedicada (em vez de injetar
 * {@code @Value} espalhado) para que todas as chaves customizadas
 * fiquem centralizadas e validadas no boot. Em produção isso também
 * facilita mock/stub nos testes unitários.</p>
 */
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /** Diretório raiz para uploads (logo das Organizations). */
    private String uploadsDir = "./uploads";

    /** Configurações específicas de upload de logo. */
    private Uploads uploads = new Uploads();

    public String getUploadsDir() {
        return uploadsDir;
    }

    public void setUploadsDir(String uploadsDir) {
        this.uploadsDir = uploadsDir;
    }

    public Uploads getUploads() {
        return uploads;
    }

    public void setUploads(Uploads uploads) {
        this.uploads = uploads;
    }

    public static class Uploads {
        private Logo logo = new Logo();
        private Attachment attachment = new Attachment();

        public Logo getLogo() {
            return logo;
        }

        public void setLogo(Logo logo) {
            this.logo = logo;
        }

        public Attachment getAttachment() {
            return attachment;
        }

        public void setAttachment(Attachment attachment) {
            this.attachment = attachment;
        }
    }

    public static class Logo {
        /**
         * Lista branca de content types aceitos para upload de logo.
         *
         * <p>SVG intencionalmente ausente: o renderer de PDF
         * (OpenHTMLtoPDF) usa Java {@code ImageIO}, que não tem
         * decoder para SVG. Aceitar SVG no upload geraria PDFs sem
         * o logo e exceções {@code IOException: Unrecognized Image
         * format} no log.</p>
         */
        private List<String> allowedContentTypes = List.of("image/png", "image/jpeg");

        public List<String> getAllowedContentTypes() {
            return allowedContentTypes;
        }

        public void setAllowedContentTypes(List<String> allowedContentTypes) {
            this.allowedContentTypes = allowedContentTypes;
        }
    }

    /**
     * Configurações de upload de anexos de boleto (PDF/imagens).
     */
    public static class Attachment {
        /**
         * Lista branca de content types aceitos para anexo de boleto.
         * Inclui PDF e as imagens renderizáveis pelo navegador.
         */
        private List<String> allowedContentTypes =
                List.of("application/pdf", "image/png", "image/jpeg");

        public List<String> getAllowedContentTypes() {
            return allowedContentTypes;
        }

        public void setAllowedContentTypes(List<String> allowedContentTypes) {
            this.allowedContentTypes = allowedContentTypes;
        }
    }
}