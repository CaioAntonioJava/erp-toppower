package br.com.toppower.erp_toppower.seller.mapper;

import br.com.toppower.erp_toppower.seller.dto.SellerCreateRequest;
import br.com.toppower.erp_toppower.seller.dto.SellerUpdateRequest;
import br.com.toppower.erp_toppower.seller.entity.Seller;
import br.com.toppower.erp_toppower.seller.enums.SellerStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários de {@link SellerMapper}.
 *
 * <p>Cobre toEntity, toResponse e applyUpdate.</p>
 */
class SellerMapperTest {

    @Test
    void toEntity_mapeiaCamposCorretamente() {
        SellerCreateRequest request = new SellerCreateRequest(
                "Carlos Vendedor", "carlos@email.com", "11999999999",
                "123.456.789-09", new BigDecimal("5.00"), SellerStatus.ATIVO);

        Seller result = SellerMapper.toEntity(request);

        assertEquals("Carlos Vendedor", result.getName());
        assertEquals("carlos@email.com", result.getEmail());
        assertEquals("11999999999", result.getPhone());
        assertEquals("123.456.789-09", result.getCpf());
        assertEquals(new BigDecimal("5.00"), result.getCommissionRate());
        assertEquals(SellerStatus.ATIVO, result.getStatus());
    }

    @Test
    void toEntity_statusNulo_naoAplicaDefault() {
        SellerCreateRequest request = new SellerCreateRequest(
                "Carlos", "carlos@email.com", "11999999999",
                "123.456.789-09", null, null);

        Seller result = SellerMapper.toEntity(request);
        assertNull(result.getStatus());
        assertNull(result.getCommissionRate());
    }

    @Test
    void toResponse_mapeiaCamposCorretamente() {
        Seller seller = new Seller();
        seller.setId(1L);
        seller.setName("Ana Vendedora");
        seller.setEmail("ana@email.com");
        seller.setPhone("11888888888");
        seller.setCpf("529.982.247-25");
        seller.setCommissionRate(new BigDecimal("10.00"));
        seller.setStatus(SellerStatus.ATIVO);

        var response = SellerMapper.toResponse(seller);

        assertEquals(1L, response.id());
        assertEquals("Ana Vendedora", response.name());
        assertEquals("ana@email.com", response.email());
        assertEquals("529.982.247-25", response.cpf());
        assertEquals(new BigDecimal("10.00"), response.commissionRate());
        assertEquals(SellerStatus.ATIVO, response.status());
    }

    @Test
    void applyUpdate_camposNaoNulos_atualiza() {
        Seller seller = new Seller();
        seller.setName("Original");
        seller.setEmail("original@email.com");
        seller.setPhone("11111111111");
        seller.setCpf("123.456.789-09");
        seller.setCommissionRate(BigDecimal.ONE);
        seller.setStatus(SellerStatus.ATIVO);

        SellerUpdateRequest update = new SellerUpdateRequest(
                "Novo Nome", "novo@email.com", "22222222222",
                "529.982.247-25", new BigDecimal("7.50"), SellerStatus.INATIVO);

        SellerMapper.applyUpdate(seller, update);

        assertEquals("Novo Nome", seller.getName());
        assertEquals("novo@email.com", seller.getEmail());
        assertEquals("22222222222", seller.getPhone());
        assertEquals("529.982.247-25", seller.getCpf());
        assertEquals(new BigDecimal("7.50"), seller.getCommissionRate());
        assertEquals(SellerStatus.INATIVO, seller.getStatus());
    }

    @Test
    void applyUpdate_camposNulos_naoAltera() {
        Seller seller = new Seller();
        seller.setName("Original");
        seller.setStatus(SellerStatus.ATIVO);

        SellerUpdateRequest update = new SellerUpdateRequest(
                null, null, null, null, null, null);

        SellerMapper.applyUpdate(seller, update);

        assertEquals("Original", seller.getName());
        assertEquals(SellerStatus.ATIVO, seller.getStatus());
    }
}
