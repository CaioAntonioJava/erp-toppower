package br.com.toppower.erp_toppower.boleto.mapper;

import br.com.toppower.erp_toppower.boleto.dto.BoletoAttachmentResponse;
import br.com.toppower.erp_toppower.boleto.entity.BoletoAttachment;

/**
 * Mapper estático entre a entidade {@link BoletoAttachment} e o DTO
 * {@link BoletoAttachmentResponse}. Segue a convenção do projeto (sem MapStruct).
 */
public final class BoletoAttachmentMapper {

    private BoletoAttachmentMapper() {
    }

    public static BoletoAttachmentResponse toResponse(BoletoAttachment attachment) {
        return new BoletoAttachmentResponse(
                attachment.getId(),
                attachment.getBoletoId(),
                attachment.getFileName(),
                attachment.getContentType(),
                attachment.getSizeBytes(),
                attachment.getPublicUrl(),
                attachment.getCreatedAt(),
                attachment.getCreatedBy()
        );
    }
}