package br.com.toppower.erp_toppower.auth.controller;

import br.com.toppower.erp_toppower.auth.dto.LoginRequest;
import br.com.toppower.erp_toppower.auth.dto.LoginResponse;
import br.com.toppower.erp_toppower.auth.dto.SwitchTenantRequest;
import br.com.toppower.erp_toppower.auth.service.AuthService;
import br.com.toppower.erp_toppower.auth.service.TenantQueryService;
import br.com.toppower.erp_toppower.security.UserDetailsImpl;
import br.com.toppower.erp_toppower.tenant.dto.TenantSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@Validated
@Tag(name = "Autenticação", description = "Endpoints de autenticação e seleção de tenant da API.")
public class AuthController {

    private final AuthService authService;
    private final TenantQueryService tenantQueryService;

    public AuthController(AuthService authService, TenantQueryService tenantQueryService) {
        this.authService = authService;
        this.tenantQueryService = tenantQueryService;
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Realizar login",
            description = "Autentica o usuário pelo e-mail e senha, selecionando o tenant (empresa) "
                    + "informado. Retorna um token JWT no formato Bearer contendo o claim 'tenant' "
                    + "que isola os dados da sessão. Use esse token no header Authorization das "
                    + "requisições protegidas."
    )
    @SecurityRequirements // endpoint público: não exige bearerAuth
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Login bem-sucedido, token JWT retornado.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = LoginResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Erro de validação (e-mail/senha/tenantUuid em formato incorreto).",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "E-mail e/ou senha inválidos, ou usuário sem acesso ao tenant.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping(value = "/tenants", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Listar tenants de um e-mail (pré-login)",
            description = "Retorna as empresas (tenants) às quais o e-mail informado está vinculado. "
                    + "Usado para popular o dropdown de seleção de empresa na tela de login, antes "
                    + "da autenticação. Retorna lista vazia se o e-mail não existir ou não tiver "
                    + "vínculos (não revela existência do e-mail)."
    )
    @SecurityRequirements // endpoint público: não exige bearerAuth
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de tenants (resumo).",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TenantSummary.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "E-mail ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    public ResponseEntity<List<TenantSummary>> listTenantsByEmail(
            @Parameter(description = "E-mail do usuário para o qual listar os tenants acessíveis.",
                    example = "caio@toppower.com.br", required = true)
            @RequestParam("email") @NotBlank(message = "email é obrigatório")
            @Email(message = "E-mail inválido") String email) {
        return ResponseEntity.ok(tenantQueryService.listTenantsByEmail(email));
    }

    @PostMapping(value = "/switch-tenant", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Trocar de tenant (empresa) na sessão corrente",
            description = "Reemite o JWT com o novo tenant, mantendo o mesmo usuário (sem re-login). "
                    + "Requer que o usuário esteja vinculado ao tenant de destino. "
                    + "Disponível para qualquer usuário autenticado."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Novo token JWT emitido com o tenant de destino.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = LoginResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token ausente/inválido, ou usuário sem acesso ao tenant de destino.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    public ResponseEntity<LoginResponse> switchTenant(@Valid @RequestBody SwitchTenantRequest request,
                                                     @AuthenticationPrincipal UserDetailsImpl current) {
        return ResponseEntity.ok(authService.switchTenant(request, current));
    }
}