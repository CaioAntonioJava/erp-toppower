package br.com.toppower.erp_toppower.boleto.repository;

import br.com.toppower.erp_toppower.boleto.entity.BoletoAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoletoAttachmentRepository extends JpaRepository<BoletoAttachment, Long>,
        JpaSpecificationExecutor<BoletoAttachment> {

    List<BoletoAttachment> findByBoletoId(Long boletoId);

    Optional<BoletoAttachment> findByIdAndBoletoId(Long id, Long boletoId);
}