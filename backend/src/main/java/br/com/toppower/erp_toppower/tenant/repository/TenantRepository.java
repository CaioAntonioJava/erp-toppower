package br.com.toppower.erp_toppower.tenant.repository;

import br.com.toppower.erp_toppower.tenant.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    boolean existsByCode(String code);

    boolean existsByCnpj(String cnpj);

    Optional<Tenant> findByCode(String code);

    Optional<Tenant> findByCnpj(String cnpj);

    /**
     * Retorna o maior código existente cuja string começa com o prefixo
     * informado (ex.: {@code "TEN"}). Usado para gerar o próximo código
     * sequencial a partir do maior já cadastrado.
     *
     * <p>Retorna {@code null} quando ainda não houver registros com o prefixo.</p>
     */
    @Query("SELECT MAX(t.code) FROM Tenant t WHERE t.code LIKE CONCAT(:prefix, '%')")
    String findMaxCodeByPrefix(@Param("prefix") String prefix);
}