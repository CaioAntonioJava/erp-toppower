package br.com.toppower.erp_toppower.boleto.exception;

public class BoletoAttachmentNotFoundException extends RuntimeException {

    public BoletoAttachmentNotFoundException(Long id) {
        super("Anexo de boleto não encontrado: " + id);
    }

    public BoletoAttachmentNotFoundException(Long boletoId, Long attachmentId) {
        super("Anexo " + attachmentId + " não pertence ao boleto " + boletoId);
    }
}