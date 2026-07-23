package br.com.toppower.erp_toppower.user.enums;

/**
 * Papéis globais de usuário no sistema.
 *
 * <p>Hierarquia de privilégios:</p>
 * <ul>
 *   <li>{@link #ROLE_ADMIN} — acesso total (administrativo + todos os módulos de negócio).</li>
 *   <li>{@link #ROLE_MANAGER} — acesso a todos os módulos de negócio (sem rotas administrativas).</li>
 *   <li>{@link #ROLE_EMPLOYEE} — acesso restrito aos módulos concedidos explicitamente.</li>
 * </ul>
 */
public enum Role {
    ROLE_ADMIN,
    ROLE_MANAGER,
    ROLE_EMPLOYEE
}
