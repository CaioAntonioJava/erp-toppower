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

    private final BoletoCreateRequest createRequest = new BoletoCreateRequest(
            "Boleto Teste",
            "Fornecedor ABC",
            new BigDecimal("1500.00"),
            LocalDate.of(2026, 8, 15),
            RegistrationStatus.ATIVO,
            10L,
            "CT-001-2026",
            LocalDate.of(2026, 8, 1),
            null,
            null);

    @Test
    void toEntity_mapeiaCamposCorretamente() {
        Boleto result = BoletoMapper.toEntity(createRequest);

        assertEquals("Boleto Teste", result.getDescription());
        assertEquals("Fornecedor ABC", result.getPayee());
        assertEquals(new BigDecimal("1500.00"), result.getValue());
        assertEquals(LocalDate.of(2026, 8, 15), result.getDueDate());
        assertEquals(RegistrationStatus.ATIVO, result.getStatus());
        assertEquals(10L, result.getSupplierId());
        assertEquals("CT-001-2026", result.getContractWorkNumber());
        assertEquals(LocalDate.of(2026, 8, 1), result.getRegistrationDate());
        assertFalse(result.isPaid());
        assertNull(result.getPaymentDate());
    }

    @Test
    void toEntity_statusNulo_naoAplicaDefault() {
        BoletoCreateRequest req = new BoletoCreateRequest(
                "Teste", "Payee", BigDecimal.TEN, LocalDate.now(), null, null,
                null, null, null, null);
        Boleto result = BoletoMapper.toEntity(req);
        assertNull(result.getStatus()); // @PrePersist da entidade que aplica default
        assertNull(result.getContractWorkNumber());
        assertNull(result.getRegistrationDate());
    }

    @Test
    void toResponse_semSupplier_supplierNameNulo() {
        Boleto boleto = new Boleto();
        boleto.setId(1L);
        boleto.setDescription("Teste");
        boleto.setPayee("Payee");
        boleto.setValue(BigDecimal.TEN);
        boleto.setDueDate(LocalDate.now());
        boleto.setStatus(RegistrationStatus.ATIVO);
        boleto.setSupplierId(null);
        boleto.setPaid(false);

        // toResponse com supplierRepository null não deve lançar exceção
        // quando supplierId é nulo
        var response = BoletoMapper.toResponse(boleto, null);
        assertEquals("Teste", response.description());
        assertNull(response.supplierName());
    }

    @Test
    void applyUpdate_camposNaoNulos_atualiza() {
        Boleto boleto = new Boleto();
        boleto.setDescription("Original");
        boleto.setPayee("Original Payee");
        boleto.setValue(BigDecimal.ONE);
        boleto.setDueDate(LocalDate.of(2026, 1, 1));
        boleto.setStatus(RegistrationStatus.ATIVO);
        boleto.setSupplierId(1L);

        BoletoUpdateRequest update = new BoletoUpdateRequest(
                "Novo", "Novo Payee", new BigDecimal("200.00"),
                LocalDate.of(2026, 12, 31), RegistrationStatus.INATIVO, 2L,
                "CT-002-2026", LocalDate.of(2026, 8, 2));

        BoletoMapper.applyUpdate(boleto, update);

        assertEquals("Novo", boleto.getDescription());
        assertEquals("Novo Payee", boleto.getPayee());
        assertEquals(new BigDecimal("200.00"), boleto.getValue());
        assertEquals(LocalDate.of(2026, 12, 31), boleto.getDueDate());
        assertEquals(RegistrationStatus.INATIVO, boleto.getStatus());
        assertEquals(2L, boleto.getSupplierId());
        assertEquals("CT-002-2026", boleto.getContractWorkNumber());
        assertEquals(LocalDate.of(2026, 8, 2), boleto.getRegistrationDate());
    }

    @Test
    void applyUpdate_camposNulos_naoAltera() {
        Boleto boleto = new Boleto();
        boleto.setDescription("Original");
        boleto.setPayee("Payee");
        boleto.setValue(BigDecimal.TEN);
        boleto.setDueDate(LocalDate.of(2026, 1, 1));
        boleto.setStatus(RegistrationStatus.ATIVO);
        boleto.setSupplierId(1L);
        boleto.setContractWorkNumber("CT-ORIGINAL");
        boleto.setRegistrationDate(LocalDate.of(2026, 1, 1));

        BoletoUpdateRequest update = new BoletoUpdateRequest(
                null, null, null, null, null, null, null, null);

        BoletoMapper.applyUpdate(boleto, update);

        assertEquals("Original", boleto.getDescription());
        assertEquals("Payee", boleto.getPayee());
        assertEquals(BigDecimal.TEN, boleto.getValue());
        assertEquals(LocalDate.of(2026, 1, 1), boleto.getDueDate());
        assertEquals(RegistrationStatus.ATIVO, boleto.getStatus());
        assertEquals(1L, boleto.getSupplierId());
        assertEquals("CT-ORIGINAL", boleto.getContractWorkNumber());
        assertEquals(LocalDate.of(2026, 1, 1), boleto.getRegistrationDate());
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
