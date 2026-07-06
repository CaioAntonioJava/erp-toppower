package br.com.toppower.erp_toppower.auth.service;

import br.com.toppower.erp_toppower.common.enums.RegistrationStatus;
import br.com.toppower.erp_toppower.tenant.dto.TenantSummary;
import br.com.toppower.erp_toppower.tenant.entity.Tenant;
import br.com.toppower.erp_toppower.tenant.repository.TenantRepository;
import br.com.toppower.erp_toppower.tenant.mapper.TenantMapper;
import br.com.toppower.erp_toppower.user.entity.User;
import br.com.toppower.erp_toppower.user.entity.UserTenant;
import br.com.toppower.erp_toppower.user.repository.UserRepository;
import br.com.toppower.erp_toppower.user.repository.UserTenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Serviço de consulta pública de tenants por email (usado pela tela de login
 * para popular o dropdown de empresas <b>antes</b> de autenticar).
 *
 * <p><b>Tradeoff consciente (info disclosure):</b> este endpoint revela a quais
 * empresas um email está vinculado. Aceitável para um ERP interno com poucos
 * tenants; se quiser mitigar, alternativa é login em 2 passos (autenticar sem
 * tenant, receber a lista, selecionar). Mantido assim por simplicidade.</p>
 */
@Service
public class TenantQueryService {

    private final UserRepository userRepository;
    private final UserTenantRepository userTenantRepository;
    private final TenantRepository tenantRepository;

    public TenantQueryService(UserRepository userRepository,
                               UserTenantRepository userTenantRepository,
                               TenantRepository tenantRepository) {
        this.userRepository = userRepository;
        this.userTenantRepository = userTenantRepository;
        this.tenantRepository = tenantRepository;
    }

    /**
     * Lista os tenants aos quais o email informado está vinculado.
     * Não revela senha nem dados sensíveis — apenas uuid, displayName, code, cnpj.
     * Retorna lista vazia se o email não existir (não revela existência do email).
     */
    @Transactional(readOnly = true)
    public List<TenantSummary> listTenantsByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(user -> listTenantsByUserUuid(user.getUuid()))
                .orElse(List.of());
    }

    @Transactional(readOnly = true)
    public List<TenantSummary> listTenantsByUserUuid(UUID userUuid) {
        List<UUID> tenantUuids = userTenantRepository.findAllByUserUuid(userUuid).stream()
                .map(UserTenant::getTenantUuid)
                .toList();
        if (tenantUuids.isEmpty()) {
            return List.of();
        }
        // Filtra apenas tenants ATIVOS — tenants inativos (placeholders de
        // bootstrap/teste) não devem aparecer no dropdown de seleção.
        List<Tenant> tenants = tenantRepository.findAllById(tenantUuids).stream()
                .filter(t -> t.getStatus() == RegistrationStatus.ATIVO)
                .toList();
        return tenants.stream().map(TenantMapper::toSummary).toList();
    }
}