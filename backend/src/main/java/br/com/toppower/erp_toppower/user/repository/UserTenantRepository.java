package br.com.toppower.erp_toppower.user.repository;

import br.com.toppower.erp_toppower.user.entity.UserTenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserTenantRepository extends JpaRepository<UserTenant, UUID> {

    /**
     * Verifica se existe o vínculo (user, tenant). Usado pelo AuthService para
     * validar, no login e no switch-tenant, que o usuário pode acessar o tenant.
     */
    boolean existsByUserUuidAndTenantUuid(UUID userUuid, UUID tenantUuid);

    /**
     * Lista todos os vínculos de um usuário. Usado para popular o dropdown de
     * empresas da tela de login (via email→userUuid) e o switcher pós-login.
     */
    List<UserTenant> findAllByUserUuid(UUID userUuid);
}