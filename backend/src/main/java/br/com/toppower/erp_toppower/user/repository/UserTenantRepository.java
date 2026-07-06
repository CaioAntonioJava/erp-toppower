package br.com.toppower.erp_toppower.user.repository;

import br.com.toppower.erp_toppower.user.entity.UserTenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * Remove todos os vínculos de um usuário via DML direto (executa
     * imediatamente, sem carregar as entidades). Usado antes de excluir o
     * usuário (hard delete), já que não há FK física entre {@code user_tenants}
     * e {@code users}, mas limpamos para não deixar vínculos órfãos.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from UserTenant ut where ut.userUuid = :userUuid")
    void deleteAllByUserUuid(@Param("userUuid") UUID userUuid);
}