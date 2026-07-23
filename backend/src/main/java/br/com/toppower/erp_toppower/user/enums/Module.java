package br.com.toppower.erp_toppower.user.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Módulos (paineis) do sistema aos quais um usuário pode ter acesso.
 *
 * <p>Cada valor vira uma {@code GrantedAuthority} (ex.: {@code MODULE_PRODUCTS})
 * em {@link br.com.toppower.erp_toppower.security.UserDetailsImpl#getAuthorities()},
 * permitindo controlar o acesso por endpoint via
 * {@code @PreAuthorize("hasAuthority('MODULE_<X>')")}.</p>
 *
 * <p>Usuários {@code ROLE_ADMIN} e {@code ROLE_MANAGER} recebem automaticamente
 * <strong>todos</strong> os módulos. Apenas {@code ROLE_EMPLOYEE} tem o acesso
 * restrito aos módulos concedidos explicitamente.</p>
 */
@Schema(name = "Module", description = "Módulo (painel) do sistema acessível a um usuário.")
public enum Module {
    // Cadastros
    MODULE_COMPANIES,
    MODULE_CUSTOMERS,
    MODULE_SUPPLIERS,
    MODULE_SELLERS,
    MODULE_PRODUCTS,

    // Comercial
    MODULE_QUOTATIONS,
    MODULE_TECHNICAL_PROPOSALS,
    MODULE_SALES_ORDERS,
    MODULE_CONTRACTS,

    // Financeiro
    MODULE_RECEIVABLES,
    MODULE_PAYABLES,
    MODULE_PURCHASES_IMPORT,
    MODULE_BOLETOS
}