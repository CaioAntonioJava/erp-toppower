package br.com.toppower.erp_toppower.boleto.mapper;

import br.com.toppower.erp_toppower.boleto.dto.BoletoCreateRequest;
import br.com.toppower.erp_toppower.boleto.dto.BoletoResponse;
import br.com.toppower.erp_toppower.boleto.dto.BoletoUpdateRequest;
import br.com.toppower.erp_toppower.boleto.entity.Boleto;
import br.com.toppower.erp_toppower.supplier.entity.Supplier;
import br.com.toppower.erp_toppower.supplier.repository.SupplierRepository;

import java.util.Optional;

/**
 * Mapper estático entre DTOs e entidade {@link Boleto}.
 * Segue a convenção do projeto (sem MapStruct).
 */
public final class BoletoMapper {

    private BoletoMapper() {
    }

    /**
     * Cria uma nova entidade a partir do request de criação.
     * O {@code status} pode ser {@code null}; o {@code @PrePersist} da
     * entidade cuida de aplicar o default {@code ATIVO}.
     */
    public static Boleto toEntity(BoletoCreateRequest request) {
        Boleto boleto = new Boleto();
        boleto.setContractWorkNumber(request.contractWorkNumber());
        boleto.setResponsibleName(request.responsibleName());
        boleto.setValue(request.value());
        boleto.setDueDate(request.dueDate());
        boleto.setStatus(request.status());
        boleto.setSupplierId(request.supplierId());
        boleto.setInvoiceNumber(request.invoiceNumber());
        boleto.setInvoiceDate(request.invoiceDate());
        boleto.setInstallmentNumber(request.installmentNumber());
        return boleto;
    }

    /**
     * Monta a resposta resolvendo o nome do fornecedor vinculado, se houver.
     */
    public static BoletoResponse toResponse(Boleto boleto, SupplierRepository supplierRepository) {
        String supplierName = null;
        if (boleto.getSupplierId() != null) {
            Optional<Supplier> supplier = supplierRepository.findById(boleto.getSupplierId());
            if (supplier.isPresent()) {
                Supplier s = supplier.get();
                supplierName = (s.getTradeName() != null && !s.getTradeName().isBlank())
                        ? s.getTradeName()
                        : s.getLegalName();
            }
        }
        return new BoletoResponse(
                boleto.getId(),
                boleto.getContractWorkNumber(),
                boleto.getResponsibleName(),
                boleto.getValue(),
                boleto.getDueDate(),
                boleto.getStatus(),
                boleto.getSupplierId(),
                supplierName,
                boleto.isPaid(),
                boleto.getPaymentDate(),
                boleto.getInvoiceNumber(),
                boleto.getInvoiceDate(),
                boleto.getInstallmentNumber(),
                boleto.getCreatedAt(),
                boleto.getUpdatedAt(),
                boleto.getCreatedBy(),
                boleto.getUpdatedBy()
        );
    }

    /**
     * Aplica uma atualização parcial (PATCH) na entidade carregada.
     * Apenas campos não nulos do request sobrescrevem o estado atual.
     * Permite limpar o supplierId enviando explicitamente o valor 0
     * (tratado como null no service) — caso contrário, null no request
     * significa "não alterar".
     */
    public static void applyUpdate(Boleto boleto, BoletoUpdateRequest request) {
        if (request.contractWorkNumber() != null) {
            // String vazia limpa o campo (convenção de PATCH parcial).
            boleto.setContractWorkNumber(request.contractWorkNumber().isBlank() ? null : request.contractWorkNumber());
        }
        if (request.responsibleName() != null) {
            boleto.setResponsibleName(request.responsibleName().isBlank() ? null : request.responsibleName());
        }
        if (request.value() != null) {
            boleto.setValue(request.value());
        }
        if (request.dueDate() != null) {
            boleto.setDueDate(request.dueDate());
        }
        if (request.status() != null) {
            boleto.setStatus(request.status());
        }
        if (request.supplierId() != null) {
            boleto.setSupplierId(request.supplierId());
        }
        if (request.invoiceNumber() != null) {
            boleto.setInvoiceNumber(request.invoiceNumber().isBlank() ? null : request.invoiceNumber());
        }
        if (request.invoiceDate() != null) {
            boleto.setInvoiceDate(request.invoiceDate());
        }
        if (request.installmentNumber() != null) {
            boleto.setInstallmentNumber(request.installmentNumber());
        }
    }
}