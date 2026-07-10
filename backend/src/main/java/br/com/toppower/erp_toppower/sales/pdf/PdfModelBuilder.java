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
    private final ImageEmbedder imageEmbedder;

    public PdfModelBuilder(OrganizationRepository organizationRepository,
                           ImageEmbedder imageEmbedder) {
        this.organizationRepository = organizationRepository;
        this.imageEmbedder = imageEmbedder;
    }

    /**
     * Monta o envelope padrão de modelo para qualquer template de PDF.
     * Inclui {@code issuer} (dados da Organization com logo embutido
     * como data URI) e {@code generatedAt} (timestamp de geração).
     * Campos específicos de cada documento (cliente, itens, totais…)
     * devem ser adicionados pelo caller.
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
     *
     * <p>O {@code logoDataUri} é populado a partir do {@code logoUrl}
     * (caminho público) lendo o arquivo do disco. Isso contorna a
     * limitação do OpenHTMLtoPDF de não resolver URLs relativas quando
     * o documento é carregado via
     * {@code PdfRendererBuilder.withHtmlContent(xhtml, baseUri)} sem
     * um {@code baseUri} bem formado. A limitação é a mesma que
     * existia no Flying Saucer (que motivou a estratégia original).
     * A solução de data URI é a forma robusta e portátil — funciona
     * em qualquer configuração de baseUri.</p>
     */
    public IssuerView resolveIssuer() {
        UUID orgUuid = OrganizationContext.require();
        Organization org = organizationRepository.findById(orgUuid)
                .orElseThrow(() -> new OrganizationNotFoundException(orgUuid));
        OrganizationResponse response = OrganizationMapper.toResponse(org);
        IssuerView base = IssuerView.from(response);
        if (base == null) return null;
        // Injeta o data URI do logo (lê do disco).
        String dataUri = imageEmbedder.toDataUri(base.logoUrl());
        return new IssuerView(
                base.corporateName(),
                base.tradeName(),
                base.cnpjFormatted(),
                base.stateRegistration(),
                base.municipalRegistration(),
                base.phone(),
                base.email(),
                base.logoUrl(),
                dataUri,
                base.addressLines()
        );
    }
}