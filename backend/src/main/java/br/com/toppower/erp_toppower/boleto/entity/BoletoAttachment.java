package br.com.toppower.erp_toppower.boleto.entity;

import br.com.toppower.erp_toppower.common.entity.OrganizationScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Anexo de um boleto (PDF ou imagem PNG/JPEG).
 *
 * <p>Herda {@link OrganizationScopedEntity} (Long + auditoria + isolamento
 * multi-tenant via {@code organization_id}). O arquivo é persistido em
 * disco sob {@code <app.uploads.dir>/attachments/boletos/}; esta entidade
 * guarda apenas os metadados e a referência (sem FK física) ao
 * {@link Boleto} via {@code boletoId}.</p>
 */
@Entity
@Table(name = "boleto_attachments")
@Getter
@Setter
@NoArgsConstructor
public class BoletoAttachment extends OrganizationScopedEntity {

    /** Identificador (Long) do Boleto ao qual o anexo pertence. Sem FK física. */
    @Column(name = "boleto_id", nullable = false)
    private Long boletoId;

    /** Nome original do arquivo enviado pelo usuário (ex.: "boleto-123.pdf"). */
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    /** Nome normalizado no disco (ex.: "15-550e8400-e29b.pdf"). */
    @Column(name = "stored_name", nullable = false, length = 255)
    private String storedName;

    /** Content-Type do arquivo (application/pdf, image/png, image/jpeg). */
    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    /** Tamanho do arquivo em bytes. */
    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    /** URL (autenticada) para baixar/exibir o anexo. */
    @Column(name = "public_url", nullable = false, length = 255)
    private String publicUrl;
}