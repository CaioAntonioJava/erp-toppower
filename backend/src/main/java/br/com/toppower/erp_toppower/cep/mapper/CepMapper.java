package br.com.toppower.erp_toppower.cep.mapper;

import br.com.toppower.erp_toppower.cep.dto.CepResponse;
import br.com.toppower.erp_toppower.cep.entity.Cep;

/**
 * Conversao {@link Cep} -> {@link CepResponse}.
 *
 * <p>Formata o CEP de 8 digitos para o padrao com hifen
 * (ex.: {@code "01310100"} -> {@code "01310-100"}) e renomeia os campos
 * internos (logradouro, bairro, cidade, uf) para os nomes do
 * {@code AddressDto} (street, neighborhood, city, state), permitindo
 * que o frontend atribua a resposta diretamente ao endereco do
 * formulario.</p>
 */
public final class CepMapper {

    private CepMapper() {
    }

    public static CepResponse toResponse(Cep cep) {
        if (cep == null) {
            return null;
        }
        String formatted = formatZipCode(cep.getCep());
        return new CepResponse(
                formatted,
                cep.getLogradouro(),
                cep.getBairro(),
                cep.getCidade(),
                cep.getUf(),
                cep.getLatitude(),
                cep.getLongitude()
        );
    }

    /**
     * Formata o CEP de 8 digitos para {@code 00000-000}.
     * Se ja vier com hifen ou em formato inesperado, retorna comoesta.
     */
    public static String formatZipCode(String cep) {
        if (cep == null) {
            return null;
        }
        String digits = cep.replaceAll("\\D", "");
        if (digits.length() == 8) {
            return digits.substring(0, 5) + "-" + digits.substring(5);
        }
        return cep;
    }
}