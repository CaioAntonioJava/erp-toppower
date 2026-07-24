package br.com.toppower.erp_toppower.contract.mapper;

import br.com.toppower.erp_toppower.contract.dto.ContractClauseRequest;
import br.com.toppower.erp_toppower.contract.dto.ContractClauseResponse;
import br.com.toppower.erp_toppower.contract.dto.ContractCreateRequest;
import br.com.toppower.erp_toppower.contract.dto.ContractUpdateRequest;
import br.com.toppower.erp_toppower.contract.entity.Contract;
import br.com.toppower.erp_toppower.contract.entity.ContractClause;
import br.com.toppower.erp_toppower.contract.enums.ContractStatus;
import br.com.toppower.erp_toppower.sales.quotation.enums.PaymentCondition;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários de {@link ContractMapper}.
 *
 * <p>Cobre toEntity, toResponse, applyUpdate e mapeamento de cláusulas.</p>
 */
class ContractMapperTest {

    @Test
    void toEntity_mapeiaCamposCorretamente() {
        ContractCreateRequest request = new ContractCreateRequest(
                1L, null, "Contrato de Prestação", "Descrição detalhada",
                LocalDate.of(2027, 1, 1), new BigDecimal("5000.00"),
                PaymentCondition.A_VISTA_DINHEIRO, null);

        Contract result = ContractMapper.toEntity(request);

        assertEquals(1L, result.getCustomerId());
        assertNull(result.getCompanyId());
        assertEquals("Contrato de Prestação", result.getTitle());
        assertEquals("Descrição detalhada", result.getDescription());
        assertEquals(new BigDecimal("5000.00"), result.getPrice());
        assertEquals(PaymentCondition.A_VISTA_DINHEIRO, result.getPaymentCondition());
        assertEquals(LocalDate.of(2027, 1, 1), result.getValidityDate());
    }

    @Test
    void toEntity_semValidityDate_naoSetado() {
        ContractCreateRequest request = new ContractCreateRequest(
                1L, null, "Título", null, null, BigDecimal.TEN, null, null);

        Contract result = ContractMapper.toEntity(request);
        assertNull(result.getValidityDate());
    }

    @Test
    void toResponse_mapeiaCamposCorretamente() {
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setCustomerId(1L);
        contract.setTitle("Contrato Teste");
        contract.setDescription("Desc");
        contract.setStatus(ContractStatus.ATIVO);
        contract.setPrice(new BigDecimal("1000.00"));
        contract.setPaymentCondition(PaymentCondition.A_VISTA_DINHEIRO);

        List<ContractClause> clauses = List.of();
        var response = ContractMapper.toResponse(contract, "Cliente ABC", "CLI000001", clauses);

        assertEquals(1L, response.id());
        assertEquals("Contrato Teste", response.title());
        assertEquals("Cliente ABC", response.clientName());
        assertEquals("CLI000001", response.clientCode());
        assertEquals("CUSTOMER", response.clientType());
        assertEquals(ContractStatus.ATIVO, response.status());
        assertTrue(response.clauses().isEmpty());
    }

    @Test
    void toResponse_clausesNulas_retornaListaVazia() {
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setCustomerId(1L);
        contract.setTitle("Teste");
        contract.setStatus(ContractStatus.ATIVO);

        var response = ContractMapper.toResponse(contract, "Cliente", "CLI001", null);
        assertTrue(response.clauses().isEmpty());
    }

    @Test
    void toResponse_companyClient_clientTypeCOMPANY() {
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setCompanyId(2L);
        contract.setTitle("Teste");
        contract.setStatus(ContractStatus.ATIVO);

        var response = ContractMapper.toResponse(contract, "Empresa", "EMP001", List.of());
        assertEquals("COMPANY", response.clientType());
    }

    @Test
    void applyUpdate_camposNaoNulos_atualiza() {
        Contract contract = new Contract();
        contract.setCustomerId(1L);
        contract.setTitle("Original");
        contract.setDescription("Desc original");
        contract.setStatus(ContractStatus.ATIVO);
        contract.setPrice(new BigDecimal("100.00"));
        contract.setPaymentCondition(PaymentCondition.PRAZO_30_DIAS);

        ContractUpdateRequest update = new ContractUpdateRequest(
                1L, 2L, "Novo Título", "Nova desc",
                ContractStatus.INATIVO, LocalDate.of(2027, 6, 1),
                new BigDecimal("200.00"), PaymentCondition.PARCELAS_30_60_90, null);

        ContractMapper.applyUpdate(contract, update);

        assertEquals(1L, contract.getCustomerId()); // null no update preserva o original
        assertEquals(2L, contract.getCompanyId());
        assertEquals("Novo Título", contract.getTitle());
        assertEquals("Nova desc", contract.getDescription());
        assertEquals(ContractStatus.INATIVO, contract.getStatus());
        assertEquals(LocalDate.of(2027, 6, 1), contract.getValidityDate());
        assertEquals(new BigDecimal("200.00"), contract.getPrice());
        assertEquals(PaymentCondition.PARCELAS_30_60_90, contract.getPaymentCondition());
    }

    @Test
    void applyUpdate_camposNulos_naoAltera() {
        Contract contract = new Contract();
        contract.setCustomerId(1L);
        contract.setTitle("Original");
        contract.setStatus(ContractStatus.ATIVO);

        ContractUpdateRequest update = new ContractUpdateRequest(
                null, null, null, null, null, null, null, null, null);

        ContractMapper.applyUpdate(contract, update);

        assertEquals(1L, contract.getCustomerId());
        assertEquals("Original", contract.getTitle());
        assertEquals(ContractStatus.ATIVO, contract.getStatus());
    }

    // ========== Cláusulas ==========

    @Test
    void toClauseEntity_mapeiaCamposCorretamente() {
        ContractClauseRequest request = new ContractClauseRequest(1, "Cláusula 1", "Conteúdo da cláusula", 10L);
        ContractClause result = ContractMapper.toClauseEntity(request, 99L);

        assertEquals(99L, result.getContractId());
        assertEquals(1, result.getClauseNumber());
        assertEquals("Cláusula 1", result.getTitle());
        assertEquals("Conteúdo da cláusula", result.getContent());
        assertEquals(10L, result.getServiceTemplateId());
    }

    @Test
    void toClauseResponse_mapeiaCamposCorretamente() {
        ContractClause clause = new ContractClause();
        clause.setId(1L);
        clause.setClauseNumber(1);
        clause.setTitle("Cláusula 1");
        clause.setContent("Conteúdo");
        clause.setServiceTemplateId(10L);

        ContractClauseResponse response = ContractMapper.toClauseResponse(clause);

        assertEquals(1L, response.id());
        assertEquals(1, response.clauseNumber());
        assertEquals("Cláusula 1", response.title());
        assertEquals("Conteúdo", response.content());
        assertEquals(10L, response.serviceTemplateId());
    }
}
