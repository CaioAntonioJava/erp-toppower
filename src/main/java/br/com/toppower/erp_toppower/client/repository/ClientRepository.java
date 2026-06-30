package br.com.toppower.erp_toppower.client.repository;

import br.com.toppower.erp_toppower.client.entity.Client;
import br.com.toppower.erp_toppower.client.enums.ClientStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientRepository extends JpaRepository<Client, UUID> {

    boolean existsByCode(String code);

    boolean existsByTaxId(String taxId);

    Optional<Client> findByCode(String code);

    Optional<Client> findByTaxId(String taxId);

    Page<Client> findByStatus(ClientStatus status, Pageable pageable);
}
