package br.com.toppower.erp_toppower.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * Handlers estáticos customizados.
 *
 * <p>Hoje servimos apenas os logos das Organizations a partir do
 * diretório configurado em {@code app.uploads.dir}/logos. O
 * {@code ResourceHandler} delega para o servlet container sem passar
 * pelo filtro de autenticação (configurado no
 * {@code SecurityConfig.PUBLIC_PATHS}).</p>
 *
 * <p>Os caminhos servidos aqui são {@code /logos/**}. O
 * {@code OrganizationLogoService} grava em {@code <uploads.dir>/logos/}
 * e armazena no banco o path público (ex.: {@code /logos/<uuid>.png}).</p>
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AppProperties appProperties;

    public WebMvcConfig(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        File logosDir = new File(appProperties.getUploadsDir(), "logos");
        // Garante que o diretório existe no boot — sem isso, o primeiro
        // request quebraria com 404 em vez de listar um diretório vazio.
        // Não falhamos se não conseguir criar: quem tenta usar já vai
        // receber erro claro ao tentar gravar.
        if (!logosDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            logosDir.mkdirs();
        }
        String absoluteLocation = logosDir.getAbsolutePath();
        if (!absoluteLocation.endsWith(File.separator)) {
            absoluteLocation += File.separator;
        }
        registry.addResourceHandler("/logos/**")
                .addResourceLocations("file:" + absoluteLocation);
    }
}