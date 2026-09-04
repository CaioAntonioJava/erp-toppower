package br.com.toppower.erp_toppower.boleto.mapper;

import br.com.toppower.erp_toppower.boleto.dto.BoletoCreateRequest;
import br.com.toppower.erp_toppower.boleto.dto.BoletoUpdateRequest;
import br.com.toppower.erp_toppower.boleto.entity.Boleto;
import br.com.toppower.erp_toppower.boleto.entity.BoletoAttachment;
import br.com.toppower.erp_toppower.common.enums.RegistrationStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários de {@link BoletoMapper} e {@link BoletoAttachmentMapper}.
 *
 * <p>Cobre:</p>
 * <ul>
 *   <li>toEntity a partir de BoletoCreateRequest</li>
 *   <li>toResponse com resolved supplier name</li>
 *   <li>applyUpdate (PATCH) com campos nulos e não nulos</li>
 *   <li>BoletoAttachmentMapper.toResponse</li>
 * </ul>
 */
class BoletoMapperTest {

    // Ordem dos campos do BoletoCreateRequest (record):
    // contractWorkNumber, responsibleName, value, dueDate, status,
    // supplierId, invoiceNumber, invoiceDate, installmentNumber,
    // installmentsCount, installmentTerms
    private final BoletoCreateRequest createRequest = new BoletoCreateRequest(
            "CT-001-2026",
            "JOAO DA SILVA",
            new BigDecimal("1500.00"),
            LocalDate.of(2026, 8, 15),
            RegistrationStatus.ATIVO,
            10L,
            "NF-00123",
            LocalDate.of(2026, 7, 20),
            1,
            null,
            null);

    @Test
    void toEntity_mapeiaCamposCorretamente() {
        Boleto result = BoletoMapper.toEntity(createRequest);

        assertEquals("CT-001-2026", result.getContractWorkNumber());
        assertEquals("JOAO DA SILVA", result.getResponsibleName());
        assertEquals(new BigDecimal("1500.00"), result.getValue());
        assertEquals(LocalDate.of(2026, 8, 15), result.getDueDate());
        assertEquals(RegistrationStatus.ATIVO, result.getStatus());
        assertEquals(10L, result.getSupplierId());
        assertEquals("NF-00123", result.getInvoiceNumber());
        assertEquals(LocalDate.of(2026, 7, 20), result.getInvoiceDate());
        assertEquals(1, result.getInstallmentNumber());
        assertFalse(result.isPaid());
        assertNull(result.getPaymentDate());
    }

    @Test
    void toEntity_statusNulo_naoAplicaDefault() {
        BoletoCreateRequest req = new BoletoCreateRequest(
                null, null, BigDecimal.TEN, LocalDate.now(), null, null,
                null, null, null, null, null);
        Boleto result = BoletoMapper.toEntity(req);
        assertNull(result.getStatus()); // @PrePersist da entidade que aplica default
        assertNull(result.getContractWorkNumber());
        assertNull(result.getResponsibleName());
    }

    @Test
    void toResponse_semSupplier_supplierNameNulo() {
        Boleto boleto = new Boleto();
        boleto.setId(1L);
        boleto.setContractWorkNumber("CT-001");
        boleto.setResponsibleName("Teste");
        boleto.setValue(BigDecimal.TEN);
        boleto.setDueDate(LocalDate.now());
        boleto.setStatus(RegistrationStatus.ATIVO);
        boleto.setSupplierId(null);
        boleto.setPaid(false);

        // toResponse com supplierRepository null não deve lançar exceção
        // quando supplierId é nulo
        var response = BoletoMapper.toResponse(boleto, null);
        assertEquals("CT-001", response.contractWorkNumber());
        assertEquals("Teste", response.responsibleName());
        assertNull(response.supplierName());
        assertNull(response.installmentPlanId());
    }

    @Test
    void applyUpdate_camposNaoNulos_atualiza() {
        Boleto boleto = new Boleto();
        boleto.setContractWorkNumber("CT-ORIGINAL");
        boleto.setResponsibleName("Original");
        boleto.setValue(BigDecimal.ONE);
        boleto.setDueDate(LocalDate.of(2026, 1, 1));
        boleto.setStatus(RegistrationStatus.ATIVO);
        boleto.setSupplierId(1L);

        // Ordem do BoletoUpdateRequest:
        // contractWorkNumber, responsibleName, value, dueDate, status,
        // supplierId, invoiceNumber, invoiceDate, installmentNumber
        BoletoUpdateRequest update = new BoletoUpdateRequest(
                "CT-002-2026", "Novo Responsável",
                new BigDecimal("200.00"), LocalDate.of(2026, 12, 31),
                RegistrationStatus.INATIVO, 2L, "NF-999", LocalDate.of(2026, 8, 2),
                3);

        BoletoMapper.applyUpdate(boleto, update);

        assertEquals("CT-002-2026", boleto.getContractWorkNumber());
        assertEquals("Novo Responsável", boleto.getResponsibleName());
        assertEquals(new BigDecimal("200.00"), boleto.getValue());
        assertEquals(LocalDate.of(2026, 12, 31), boleto.getDueDate());
        assertEquals(RegistrationStatus.INATIVO, boleto.getStatus());
        assertEquals(2L, boleto.getSupplierId());
        assertEquals("NF-999", boleto.getInvoiceNumber());
        assertEquals(LocalDate.of(2026, 8, 2), boleto.getInvoiceDate());
        assertEquals(3, boleto.getInstallmentNumber());
    }

    @Test
    void applyUpdate_camposNulos_naoAltera() {
        Boleto boleto = new Boleto();
        boleto.setContractWorkNumber("CT-ORIGINAL");
        boleto.setResponsibleName("Original");
        boleto.setValue(BigDecimal.TEN);
        boleto.setDueDate(LocalDate.of(2026, 1, 1));
        boleto.setStatus(RegistrationStatus.ATIVO);
        boleto.setSupplierId(1L);
        boleto.setInvoiceNumber("NF-ORIG");
        boleto.setInvoiceDate(LocalDate.of(2026, 1, 1));
        boleto.setInstallmentNumber(1);

        // Todos os campos nulos → nenhum campo deve ser alterado.
        BoletoUpdateRequest update = new BoletoUpdateRequest(
                null, null, null, null, null, null, null, null, null);

        BoletoMapper.applyUpdate(boleto, update);

        assertEquals("CT-ORIGINAL", boleto.getContractWorkNumber());
        assertEquals("Original", boleto.getResponsibleName());
        assertEquals(BigDecimal.TEN, boleto.getValue());
        assertEquals(LocalDate.of(2026, 1, 1), boleto.getDueDate());
        assertEquals(RegistrationStatus.ATIVO, boleto.getStatus());
        assertEquals(1L, boleto.getSupplierId());
        assertEquals("NF-ORIG", boleto.getInvoiceNumber());
        assertEquals(LocalDate.of(2026, 1, 1), boleto.getInvoiceDate());
        assertEquals(1, boleto.getInstallmentNumber());
    }

    @Test
    void applyUpdate_stringVazia_limpaCamposTexto() {
        Boleto boleto = new Boleto();
        boleto.setContractWorkNumber("CT-ORIGINAL");
        boleto.setResponsibleName("Original");
        boleto.setInvoiceNumber("NF-ORIG");

        // Strings vazias devem limpar (setar null) os campos de texto.
        BoletoUpdateRequest update = new BoletoUpdateRequest(
                "", "", null, null, null, null, "", null, null);

        BoletoMapper.applyUpdate(boleto, update);

        assertNull(boleto.getContractWorkNumber());
        assertNull(boleto.getResponsibleName());
        assertNull(boleto.getInvoiceNumber());
    }

    // ========== BoletoAttachmentMapper ==========

    @Test
    void attachmentToResponse_mapeiaCamposCorretamente() {
        BoletoAttachment attachment = new BoletoAttachment();
        attachment.setId(99L);
        attachment.setBoletoId(1L);
        attachment.setFileName("boleto.pdf");
        attachment.setContentType("application/pdf");
        attachment.setSizeBytes(1024L);
        attachment.setPublicUrl("/uploads/boletos/99/file.pdf");

        var response = BoletoAttachmentMapper.toResponse(attachment);

        assertEquals(99L, response.id());
        assertEquals(1L, response.boletoId());
        assertEquals("boleto.pdf", response.fileName());
        assertEquals("application/pdf", response.contentType());
        assertEquals(1024L, response.sizeBytes());
        assertEquals("/uploads/boletos/99/file.pdf", response.publicUrl());
    }
}