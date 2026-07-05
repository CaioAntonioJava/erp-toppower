package br.com.toppower.erp_toppower.cep.service;

import br.com.toppower.erp_toppower.cep.dto.CepResponse;
import br.com.toppower.erp_toppower.cep.entity.Cep;
import br.com.toppower.erp_toppower.cep.exception.CepNotFoundException;
import br.com.toppower.erp_toppower.cep.mapper.CepMapper;
import br.com.toppower.erp_toppower.cep.repository.CepRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

/**
 * Servico de lookup de CEP na base local (offline, sem API externa).
 */
@Service
public class CepService {

    /** CEP valido: 8 digitos, com ou sem hifen. */
    private static final Pattern CEP_PATTERN = Pattern.compile("\\d{5}-?\\d{3}");

    private final CepRepository cepRepository;

    public CepService(CepRepository cepRepository) {
        this.cepRepository = cepRepository;
    }

    /**
     * Busca o endereco pelo CEP na base local.
     *
     * @param cep CEP em qualquer formato (00000-000 ou 00000000).
     * @return {@link CepResponse} com os campos do endereco.
     * @throws IllegalArgumentException se o CEP estiver fora do formato.
     * @throws CepNotFoundException      se o CEP nao existir na base.
     */
    @Transactional(readOnly = true)
    public CepResponse findByCep(String cep) {
        String normalized = normalize(cep);
        Cep entity = cepRepository.findById(normalized)
                .orElseThrow(() -> new CepNotFoundException(normalized));
        return CepMapper.toResponse(entity);
    }

    /**
     * Quantidade de CEPs carregados na base local. Usado pelo endpoint
     * de status ({@code GET /api/v1/ceps/count}).
     */
    @Transactional(readOnly = true)
    public long count() {
        return cepRepository.count();
    }

    /**
     * Normaliza o CEP removendo hifen/espacos -> 8 digitos.
     * Lanca {@link IllegalArgumentException} se o resultado nao for valido.
     */
    private String normalize(String cep) {
        if (cep == null) {
            throw new IllegalArgumentException("CEP não pode ser nulo.");
        }
        String trimmed = cep.trim();
        if (!CEP_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException(
                    "CEP inválido. Use o formato 00000-000 ou 8 dígitos.");
        }
        return trimmed.replace("-", "");
    }
}