package br.com.toppower.erp_toppower.cep.mapper;

import br.com.toppower.erp_toppower.cep.entity.Cep;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários de {@link CepMapper}.
 *
 * <p>Cobre:</p>
 * <ul>
 *   <li>toResponse com CEP válido e nulo</li>
 *   <li>formatZipCode com diversos formatos de entrada</li>
 * </ul>
 */
class CepMapperTest {

    @Test
    void toResponse_mapeiaCamposCorretamente() {
        Cep cep = new Cep();
        cep.setCep("01310100");
        cep.setStreet("Avenida Paulista");
        cep.setNeighborhood("Bela Vista");
        cep.setCity("São Paulo");
        cep.setState("SP");
        cep.setLatitude(new BigDecimal("-23.561"));
        cep.setLongitude(new BigDecimal("-46.656"));

        var response = CepMapper.toResponse(cep);

        assertEquals("01310-100", response.zipCode());
        assertEquals("Avenida Paulista", response.street());
        assertEquals("Bela Vista", response.neighborhood());
        assertEquals("São Paulo", response.city());
        assertEquals("SP", response.state());
        assertEquals(new BigDecimal("-23.561"), response.latitude());
        assertEquals(new BigDecimal("-46.656"), response.longitude());
    }

    @Test
    void toResponse_cepNulo_retornaNull() {
        assertNull(CepMapper.toResponse(null));
    }

    @Test
    void toResponse_cepComHifen_preservaFormatacao() {
        Cep cep = new Cep();
        cep.setCep("01310-100");
        cep.setCity("São Paulo");
        cep.setState("SP");

        var response = CepMapper.toResponse(cep);
        assertEquals("01310-100", response.zipCode());
    }

    @Test
    void formatZipCode_8Digitos_formataComHifen() {
        assertEquals("01310-100", CepMapper.formatZipCode("01310100"));
    }

    @Test
    void formatZipCode_jaFormatado_preserva() {
        assertEquals("01310-100", CepMapper.formatZipCode("01310-100"));
    }

    @Test
    void formatZipCode_nulo_retornaNull() {
        assertNull(CepMapper.formatZipCode(null));
    }

    @Test
    void formatZipCode_menosDe8Digitos_retornaOriginal() {
        assertEquals("123", CepMapper.formatZipCode("123"));
    }

    @Test
    void formatZipCode_maisDe8Digitos_retornaOriginal() {
        assertEquals("123456789", CepMapper.formatZipCode("123456789"));
    }

    @Test
    void formatZipCode_comCaracteresNaoDigitos_extraiApenasDigitos() {
        assertEquals("01310-100", CepMapper.formatZipCode("01310.100"));
    }
}
