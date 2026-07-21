package br.com.toppower.erp_toppower.boleto.mapper;

import br.com.toppower.erp_toppower.boleto.dto.BoletoCreateRequest;
import br.com.toppower.erp_toppower.boleto.dto.BoletoResponse;
import br.com.toppower.erp_toppower.boleto.dto.BoletoUpdateRequest;
import br.com.toppower.erp_toppower.boleto.entity.Boleto;

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
        boleto.setDescription(request.description());
        boleto.setPayee(request.payee());
        boleto.setValue(request.value());
        boleto.setDueDate(request.dueDate());
        boleto.setStatus(request.status());
        return boleto;
    }

    public static BoletoResponse toResponse(Boleto boleto) {
        return new BoletoResponse(
                boleto.getId(),
                boleto.getDescription(),
                boleto.getPayee(),
                boleto.getValue(),
                boleto.getDueDate(),
                boleto.getStatus(),
                boleto.getCreatedAt(),
                boleto.getUpdatedAt(),
                boleto.getCreatedBy(),
                boleto.getUpdatedBy()
        );
    }

    /**
     * Aplica uma atualização parcial (PATCH) na entidade carregada.
     * Apenas campos não nulos do request sobrescrevem o estado atual.
     */
    public static void applyUpdate(Boleto boleto, BoletoUpdateRequest request) {
        if (request.description() != null) {
            boleto.setDescription(request.description());
        }
        if (request.payee() != null) {
            boleto.setPayee(request.payee());
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
    }
}