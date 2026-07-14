package br.com.toppower.erp_toppower.userorganization.repository;

import br.com.toppower.erp_toppower.userorganization.entity.UserOrganization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserOrganizationRepository extends JpaRepository<UserOrganization, Long> {

    List<UserOrganization> findByUserId(Long userId);

    Optional<UserOrganization> findByUserIdAndOrganizationId(Long userId, Long organizationId);

    boolean existsByUserIdAndOrganizationId(Long userId, Long organizationId);

    Optional<UserOrganization> findFirstByUserIdAndIsDefaultTrue(Long userId);

    /**
     * Lista as Organizations de um usuário cuja Organization esteja ATIVA,
     * ordenadas pela razão social. Usado para montar a lista do seletor.
     */
    @Query("""
            SELECT uo FROM UserOrganization uo
              JOIN FETCH uo.organization o
            WHERE uo.user.id = :userId
              AND o.status = br.com.toppower.erp_toppower.organization.enums.OrganizationStatus.ATIVO
            ORDER BY o.corporateName
            """)
    List<UserOrganization> findActiveByUserId(@Param("userId") Long userId);
}