package br.com.toppower.erp_toppower.profile.repository;

import br.com.toppower.erp_toppower.profile.entity.Profile;
import br.com.toppower.erp_toppower.profile.enums.ProfileStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long> {

    boolean existsByCpf(String cpf);

    boolean existsByEmail(String email);

    boolean existsByUserId(Long userId);

    Optional<Profile> findByUserId(Long userId);

    Page<Profile> findByStatus(ProfileStatus status, Pageable pageable);
}
