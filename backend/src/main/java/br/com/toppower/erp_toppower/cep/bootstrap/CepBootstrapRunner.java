package br.com.toppower.erp_toppower.cep.bootstrap;

import br.com.toppower.erp_toppower.cep.dto.CepImportResult;
import br.com.toppower.erp_toppower.cep.repository.CepRepository;
import br.com.toppower.erp_toppower.cep.service.CepImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Runner que importa automaticamente a base local de CEPs (CSV embarcado)
 * na primeira inicialização em que a tabela {@code ceps} estiver vazia.
 *
 * <p>Motivo: o lookup offline ({@code GET /api/v1/ceps/{cep}}) depende de a
 * base ter sido carregada via {@code POST /api/v1/ceps/import}. Sem este
 * runner, o admin precisa lembrar de chamar o endpoint manualmente após
 * instalar o sistema — caso contrário, todo lookup retorna 404 e nenhum
 * formulário tem preenchimento automático de endereço.</p>
 *
 * <p>Comportamento:</p>
 * <ul>
 *   <li>Roda após o {@code BootstrapRunner} ({@link Order}(20)), pois
 *       a tabela {@code ceps} é global e independe do bootstrap de admin.</li>
 *   <li>Se a tabela já tiver registros, é no-op (não reimporta).</li>
 *   <li>Se a tabela estiver vazia e {@code app.cep.import.auto=true}
 *       (default), dispara {@link CepImportService#importFromCsv(boolean)}
 *       com {@code force=false}.</li>
 *   <li>Em caso de falha (CSV ausente, erro de IO), apenas loga warning
 *       e segue — não impede o boot. O admin pode importar manualmente
 *       depois via endpoint.</li>
 * </ul>
 *
 * <p>Desabilitar via {@code CEP_AUTO_IMPORT=false} no ambiente.</p>
 */
@Component
@Order(20)
public class CepBootstrapRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CepBootstrapRunner.class);

    private final CepRepository cepRepository;
    private final CepImportService cepImportService;

    @Value("${app.cep.import.auto:true}")
    private boolean autoImport;

    public CepBootstrapRunner(CepRepository cepRepository, CepImportService cepImportService) {
        this.cepRepository = cepRepository;
        this.cepImportService = cepImportService;
    }

    @Override
    public void run(String... args) {
        if (!autoImport) {
            log.debug("Auto-import de CEPs desabilitado (app.cep.import.auto=false).");
            return;
        }
        try {
            long existing = cepRepository.count();
            if (existing > 0) {
                log.debug("Base de CEPs já populada com {} registros — pulando auto-import.", existing);
                return;
            }
            log.info("Base de CEPs vazia. Iniciando auto-import do CSV embarcado...");
            CepImportResult result = cepImportService.importFromCsv(false);
            log.info("Auto-import de CEPs concluído: {} inseridos, {} duplicados, {} erros, {} ms.",
                    result.imported(), result.duplicatesIgnored(), result.errors(), result.durationMs());
        } catch (Exception e) {
            // Falha no auto-import não deve impedir o boot — o admin pode
            // disparar POST /api/v1/ceps/import manualmente quando o CSV
            // estiver disponível.
            log.warn("Auto-import de CEPs falhou (o sistema segue normalmente): {}", e.getMessage());
        }
    }
}