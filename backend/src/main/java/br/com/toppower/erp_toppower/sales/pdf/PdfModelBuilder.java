package br.com.toppower.erp_toppower.sales.pdf;

import br.com.toppower.erp_toppower.organization.dto.OrganizationResponse;
import br.com.toppower.erp_toppower.organization.entity.Organization;
import br.com.toppower.erp_toppower.organization.exception.OrganizationNotFoundException;
import br.com.toppower.erp_toppower.organization.repository.OrganizationRepository;
import br.com.toppower.erp_toppower.organization.mapper.OrganizationMapper;
import br.com.toppower.erp_toppower.common.context.OrganizationContext;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Constrói o {@code Map<String, Object>} que alimenta os templates
 * Thymeleaf dos PDFs (cotação, proposta técnica, pedido de venda).
 *
 * <p>Centraliza a leitura da Organization ativa via
 * {@link OrganizationContext} para que os endpoints fiquem limpos e o
 * template receba um {@link IssuerView} já pronto para renderizar.</p>
 */
@Component
public class PdfModelBuilder {

    private final OrganizationRepository organizationRepository;

    public PdfModelBuilder(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    /**
     * Monta o envelope padrão de modelo para qualquer template de PDF.
     * Inclui {@code issuer} (dados da Organization) e {@code generatedAt}
     * (timestamp de geração). Campos específicos de cada documento
     * (cliente, itens, totais…) devem ser adicionados pelo caller.
     */
    public Map<String, Object> buildBaseModel() {
        Map<String, Object> model = new HashMap<>();
        model.put("issuer", resolveIssuer());
        model.put("generatedAt", Instant.now());
        return model;
    }

    /**
     * Resolve a Organization ativa e a transforma em {@link IssuerView}.
     * Lança {@link OrganizationNotFoundException} se o contexto não
     * bater com nenhuma org cadastrada (situação anômala que não deveria
     * acontecer em produção).
     */
    public IssuerView resolveIssuer() {
        UUID orgUuid = OrganizationContext.require();
        Organization org = organizationRepository.findById(orgUuid)
                .orElseThrow(() -> new OrganizationNotFoundException(orgUuid));
        OrganizationResponse response = OrganizationMapper.toResponse(org);
        return IssuerView.from(response);
    }
}