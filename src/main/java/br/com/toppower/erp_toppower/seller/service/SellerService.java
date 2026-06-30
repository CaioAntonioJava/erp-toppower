package br.com.toppower.erp_toppower.seller.service;

import br.com.toppower.erp_toppower.common.dto.PagedResponse;
import br.com.toppower.erp_toppower.seller.dto.SellerCreateRequest;
import br.com.toppower.erp_toppower.seller.dto.SellerResponse;
import br.com.toppower.erp_toppower.seller.dto.SellerUpdateRequest;
import br.com.toppower.erp_toppower.seller.entity.Seller;
import br.com.toppower.erp_toppower.seller.exception.DuplicateSellerCpfException;
import br.com.toppower.erp_toppower.seller.exception.DuplicateSellerEmailException;
import br.com.toppower.erp_toppower.seller.exception.SellerNotFoundException;
import br.com.toppower.erp_toppower.seller.mapper.SellerMapper;
import br.com.toppower.erp_toppower.seller.repository.SellerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class SellerService {

    private final SellerRepository sellerRepository;

    public SellerService(SellerRepository sellerRepository) {
        this.sellerRepository = sellerRepository;
    }

    @Transactional
    public SellerResponse create(SellerCreateRequest request) {
        if (sellerRepository.existsByCpf(request.cpf())) {
            throw new DuplicateSellerCpfException(request.cpf());
        }
        if (sellerRepository.existsByEmail(request.email())) {
            throw new DuplicateSellerEmailException(request.email());
        }
        Seller seller = SellerMapper.toEntity(request);
        Seller saved = sellerRepository.save(seller);
        return SellerMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PagedResponse<SellerResponse> getAll(Pageable pageable) {
        Page<SellerResponse> mapped = sellerRepository.findAll(pageable)
                .map(SellerMapper::toResponse);
        return PagedResponse.from(mapped);
    }

    @Transactional(readOnly = true)
    public SellerResponse getById(UUID id) {
        return sellerRepository.findById(id)
                .map(SellerMapper::toResponse)
                .orElseThrow(() -> new SellerNotFoundException(id));
    }

    @Transactional
    public SellerResponse update(UUID id, SellerUpdateRequest request) {
        Seller seller = sellerRepository.findById(id)
                .orElseThrow(() -> new SellerNotFoundException(id));

        if (request.cpf() != null && !request.cpf().equals(seller.getCpf())) {
            if (sellerRepository.existsByCpf(request.cpf())) {
                throw new DuplicateSellerCpfException(request.cpf());
            }
        }

        if (request.email() != null && !request.email().equals(seller.getEmail())) {
            if (sellerRepository.existsByEmail(request.email())) {
                throw new DuplicateSellerEmailException(request.email());
            }
        }

        SellerMapper.applyUpdate(seller, request);
        Seller saved = sellerRepository.save(seller);
        return SellerMapper.toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        if (!sellerRepository.existsById(id)) {
            throw new SellerNotFoundException(id);
        }
        sellerRepository.deleteById(id);
    }
}
