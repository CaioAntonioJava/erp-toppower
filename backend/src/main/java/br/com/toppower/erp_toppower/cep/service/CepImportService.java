package br.com.toppower.erp_toppower.cep.service;

import br.com.toppower.erp_toppower.cep.dto.CepImportResult;
import br.com.toppower.erp_toppower.cep.repository.CepRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Importador do CSV de CEPs para a tabela {@code ceps}, via
 * {@link JdbcTemplate#batchUpdate} com {@code INSERT IGNORE} —
 * performatico para ~850 mil registros.
 *
 * <p><strong>Suporta dois formatos (deteccao automatica):</strong></p>
 * <ol>
 *   <li><strong>TSV sem header</strong> — {@code cep<TAB>state<TAB>city<TAB>neighborhood<TAB>street}.
 *       Formato do arquivo embarcado {@code cep/ceps_brasil.csv} (base
 *       utfcepos, UTF-8). Detectado quando a 1a linha nao contem nomes de
 *       coluna mapeaveis (ex.: o 1o campo ja e um CEP de 8 digitos).</li>
 *   <li><strong>CSV com header</strong> — separador {@code ;} ou @{@code ,},
 *       mapeia colunas pelos nomes aceitando sinonimos
 *       (logradouro/rua/street, uf/estado/state, etc.). Formato CEP Aberto.</li>
 * </ol>
 *
 * <p>O caminho do CSV vem de {@code app.cep.import.csv-path} e aceita:</p>
 * <ul>
 *   <li>{@code classpath:cep/ceps_brasil.csv} — recurso embarcado no JAR (default)</li>
 *   <li>caminho absoluto no filesystem (ex.: {@code C:/base/ceps.csv})</li>
 * </ul>
 *
 * <p>Disparado por um ADMIN via {@code POST /api/v1/ceps/import}. A base
 * persiste entre reinicios (ddl-auto=update).</p>
 */
@Service
public class CepImportService {

    private static final Logger log = LoggerFactory.getLogger(CepImportService.class);

    /** CEP valido apos normalizacao: 8 digitos. */
    private static final Pattern CEP_DIGITS = Pattern.compile("\\d{8}");

    /** Sinonimos de colunas aceitos no header do CSV (formato 2). */
    private static final Map<String, String> COLUMN_ALIASES = Map.ofEntries(
            Map.entry("cep", "cep"),
            Map.entry("logradouro", "street"),
            Map.entry("rua", "street"),
            Map.entry("street", "street"),
            Map.entry("bairro", "neighborhood"),
            Map.entry("neighborhood", "neighborhood"),
            Map.entry("cidade", "city"),
            Map.entry("city", "city"),
            Map.entry("uf", "state"),
            Map.entry("estado", "state"),
            Map.entry("state", "state"),
            Map.entry("latitude", "latitude"),
            Map.entry("lat", "latitude"),
            Map.entry("longitude", "longitude"),
            Map.entry("lng", "longitude"),
            Map.entry("lon", "longitude")
    );

    private final CepRepository cepRepository;
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.cep.import.csv-path:classpath:cep/ceps_brasil.csv}")
    private String csvPath;

    @Value("${app.cep.import.batch-size:1000}")
    private int batchSize;

    public CepImportService(CepRepository cepRepository, JdbcTemplate jdbcTemplate) {
        this.cepRepository = cepRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Importa o CSV apontado por {@code app.cep.import.csv-path}.
     *
     * @param force se {@code true} e a base ja estiver populada, trunca
     *              (deleteAllInBatch) e reimporta; se {@code false} e
     *              houver dados, aborta com {@link IllegalStateException}.
     * @return estatisticas da importacao.
     */
    @Transactional
    public CepImportResult importFromCsv(boolean force) {
        if (csvPath == null || csvPath.isBlank()) {
            throw new IllegalStateException(
                    "Caminho do CSV nao configurado. Defina CEP_CSV_PATH no .env "
                            + "ou a propriedade app.cep.import.csv-path.");
        }

        long existing = cepRepository.count();
        if (existing > 0 && !force) {
            throw new IllegalStateException(
                    "Base de CEPs ja populada com " + existing
                            + " registros. Use ?force=true para truncar e reimportar.");
        }
        if (existing > 0) {
            log.info("force=true: removendo {} CEPs existentes antes de reimportar.", existing);
            cepRepository.deleteAllInBatch();
        }

        long start = System.currentTimeMillis();
        long totalLines = 0;
        long imported = 0;
        long errors = 0;
        long duplicatesIgnored = 0;

        String sql = "INSERT IGNORE INTO ceps (cep, street, neighborhood, city, state, latitude, longitude) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (BufferedReader reader = openReader(csvPath)) {
            String firstLine = reader.readLine();
            if (firstLine == null) {
                throw new IllegalStateException("CSV vazio.");
            }

            // Detecta o formato pela 1a linha.
            char separator = detectSeparator(firstLine);
            boolean hasHeader = isHeaderLine(firstLine, separator);

            Map<String, Integer> colIndex = hasHeader
                    ? mapHeader(firstLine, separator)
                    : Map.of("cep", 0, "state", 1, "city", 2, "neighborhood", 3, "street", 4);
            requireColumn(colIndex, "cep");

            List<Object[]> batch = new ArrayList<>(batchSize);

            // Se a 1a linha nao e header, ela e um registro valido — processa.
            String line = hasHeader ? reader.readLine() : firstLine;
            while (line != null) {
                if (!line.isBlank()) {
                    totalLines++;
                    Object[] row = parseLine(line, separator, colIndex);
                    if (row == null) {
                        errors++;
                    } else {
                        batch.add(row);
                        if (batch.size() >= batchSize) {
                            int[] results = jdbcTemplate.batchUpdate(sql, batch);
                            int inserted = countInserted(results);
                            imported += inserted;
                            duplicatesIgnored += batch.size() - inserted;
                            batch.clear();
                        }
                    }
                }
                line = reader.readLine();
            }
            if (!batch.isEmpty()) {
                int[] results = jdbcTemplate.batchUpdate(sql, batch);
                int inserted = countInserted(results);
                imported += inserted;
                duplicatesIgnored += batch.size() - inserted;
            }
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao ler o CSV: " + e.getMessage(), e);
        }

        long durationMs = System.currentTimeMillis() - start;
        log.info("Importacao de CEPs concluida: {} lidos, {} inseridos, {} duplicados, {} erros, {} ms.",
                totalLines, imported, duplicatesIgnored, errors, durationMs);
        return new CepImportResult(totalLines, imported, duplicatesIgnored, errors, durationMs);
    }

    /**
     * Abre um {@link BufferedReader} para o CSV, suportando caminhos
     * {@code classpath:...} (recurso embarcado no JAR) e caminhos no
     * filesystem. Detecta encoding (UTF-8 com fallback ISO-8859-1).
     */
    private BufferedReader openReader(String path) throws IOException {
        Resource resource = resolveResource(path);
        InputStream stream = resource.getInputStream();
        Charset charset = detectCharset(stream);
        // Reabre o stream apos detectar o charset (detectCharset consome o inicio).
        stream.close();
        stream = resource.getInputStream();
        return new BufferedReader(new InputStreamReader(stream, charset));
    }

    /** Resolve um caminho para um {@link Resource}, suportando classpath: e filesystem. */
    private Resource resolveResource(String path) throws IOException {
        if (path.startsWith("classpath:")) {
            String location = path.substring("classpath:".length());
            ClassPathResource cpr = new ClassPathResource(location);
            if (!cpr.exists()) {
                throw new IllegalStateException(
                        "Recurso classpath nao encontrado: " + path);
            }
            return cpr;
        }
        // Filesystem: usa UrlResource para caminhos absolutos/relativos.
        Path p = Paths.get(path);
        if (!Files.isReadable(p)) {
            throw new IllegalStateException(
                    "Arquivo CSV nao encontrado ou ilegivel: " + p.toAbsolutePath());
        }
        return new UrlResource(p.toUri());
    }

    /**
     * Decide se a linha parece um header (nao e um CEP no 1o campo).
     * TSV sem header tem a 1a coluna = CEP de 8 digitos.
     */
    private boolean isHeaderLine(String line, char separator) {
        String first = firstField(line, separator);
        if (first == null || first.isBlank()) {
            return false;
        }
        // Se o 1o campo for um CEP valido (8 digitos), nao e header.
        return normalizeCep(first) == null;
    }

    private String firstField(String line, char separator) {
        int idx = line.indexOf(separator);
        return idx >= 0 ? line.substring(0, idx).trim() : line.trim();
    }

    /**
     * Quebra uma linha em colunas e monta o array de parametros para o
     * INSERT. Retorna {@code null} se o CEP for invalido.
     */
    private Object[] parseLine(String line, char separator, Map<String, Integer> colIndex) {
        String[] fields = splitCsv(line, separator);
        String cep = normalizeCep(valueOrNull(fields, colIndex.get("cep")));
        if (cep == null) {
            return null;
        }
        String street = valueOrNull(fields, colIndex.get("street"));
        String neighborhood = valueOrNull(fields, colIndex.get("neighborhood"));
        String city = valueOrNull(fields, colIndex.get("city"));
        String state = valueOrNull(fields, colIndex.get("state"));
        if (state != null) {
            state = state.trim().toUpperCase();
            if (state.length() != 2) {
                state = null;
            }
        }
        if (city == null || city.isBlank()) {
            return null; // city e NOT NULL na entidade
        }
        BigDecimal lat = toBigDecimal(valueOrNull(fields, colIndex.get("latitude")));
        BigDecimal lng = toBigDecimal(valueOrNull(fields, colIndex.get("longitude")));
        return new Object[]{cep, street, neighborhood, city, state, lat, lng};
    }

    /**
     * Mapeia o header do CSV para os nomes canonicos (cep, street,
     * neighborhood, city, state, latitude, longitude), aceitando sinonimos.
     */
    private Map<String, Integer> mapHeader(String headerLine, char separator) {
        String[] headers = splitCsv(headerLine, separator);
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            String raw = headers[i] == null ? "" : headers[i].trim().toLowerCase().replace("\"", "");
            String canonical = COLUMN_ALIASES.get(raw);
            if (canonical != null && !index.containsKey(canonical)) {
                index.put(canonical, i);
            }
        }
        return index;
    }

    private void requireColumn(Map<String, Integer> colIndex, String name) {
        if (!colIndex.containsKey(name)) {
            throw new IllegalStateException(
                    "Coluna obrigatoria '" + name + "' nao encontrada no CSV. "
                            + "Colunas reconhecidas: " + colIndex.keySet());
        }
    }

    /** Conta quantos INSERTs foram efetivos (1) vs. ignorados (0). */
    private int countInserted(int[] results) {
        int count = 0;
        for (int r : results) {
            if (r >= 1 || r == java.sql.Statement.SUCCESS_NO_INFO) {
                count++;
            }
        }
        return count;
    }

    /**
     * Split simples de CSV/TSV sem aspas aninhadas. Suficiente para as
     * bases suportadas (utfcepos e CEP Aberto nao usam aspas).
     */
    private String[] splitCsv(String line, char separator) {
        List<String> out = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == separator) {
                out.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        out.add(sb.toString().trim());
        return out.toArray(new String[0]);
    }

    /**
     * Detecta o separador: tab se houver tabs, senao ';' ou ',' (o mais
     * frequente na 1a linha).
     */
    private char detectSeparator(String line) {
        long tabs = line.chars().filter(c -> c == '\t').count();
        if (tabs > 0) return '\t';
        long semicolons = line.chars().filter(c -> c == ';').count();
        long commas = line.chars().filter(c -> c == ',').count();
        return semicolons >= commas ? ';' : ',';
    }

    /**
     * Detecta o encoding: le os primeiros 2KB como UTF-8; se houver
     * U+FFFD (char de reposicao), assume ISO-8859-1. Reabre o stream
     * apos (o caller consome o stream de novo).
     */
    private Charset detectCharset(InputStream stream) throws IOException {
        byte[] buf = new byte[2048];
        int n = stream.read(buf);
        if (n > 0) {
            String sample = new String(buf, 0, n, StandardCharsets.UTF_8);
            if (sample.indexOf('\uFFFD') >= 0) {
                return StandardCharsets.ISO_8859_1;
            }
        }
        return StandardCharsets.UTF_8;
    }

    private String normalizeCep(String raw) {
        if (raw == null) {
            return null;
        }
        String digits = raw.replaceAll("\\D", "");
        if (CEP_DIGITS.matcher(digits).matches()) {
            return digits;
        }
        return null;
    }

    private String valueOrNull(String[] fields, Integer idx) {
        if (idx == null || idx < 0 || idx >= fields.length) {
            return null;
        }
        String v = fields[idx];
        if (v == null || v.isBlank() || "null".equalsIgnoreCase(v)) {
            return null;
        }
        return v.trim();
    }

    private BigDecimal toBigDecimal(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(raw.replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}