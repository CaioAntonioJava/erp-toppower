package br.com.toppower.erp_toppower.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração global do SpringDoc OpenAPI.
 * Define metadados da API e o esquema de segurança {@code bearerAuth} (JWT)
 * que será referenciado pelos endpoints protegidos.
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI erpToppowerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ERP TOPPOWER API")
                        .version("v1")
                        .description("API REST do sistema ERP TOPPOWER para " +
                                "gestão de usuários, autenticação JWT e outros módulos empresariais.")
                        .contact(new Contact()
                                .name("CAIO HENRIQUE ANTONIO")
                                .email("caio@toppowermateriais.com.br"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://toppower.com.br")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Insira apenas o token JWT (sem o prefixo 'Bearer ').")));
    }
}
