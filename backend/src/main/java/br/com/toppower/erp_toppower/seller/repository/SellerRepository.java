package br.com.toppower.erp_toppower.seller.repository;

import br.com.toppower.erp_toppower.seller.entity.Seller;
import br.com.toppower.erp_toppower.seller.enums.SellerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SellerRepository extends JpaRepository<Seller, UUID> {

    boolean existsByCpf(String cpf);

    boolean existsByEmail(String email);

    Optional<Seller> findByCpf(String cpf);

    Optional<Seller> findByEmail(String email);

    Page<Seller> findByStatus(SellerStatus status, Pageable pageable);
}
