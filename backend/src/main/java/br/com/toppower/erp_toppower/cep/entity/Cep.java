package br.com.toppower.erp_toppower.cep.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Registro de CEP da base local (offline) para preenchimento automatico
 * de enderecos, sem consulta a APIs externas.
 *
 * <p>Entidade leve e intencionalmente <strong>nao</strong> herda de
 * {@code BaseEntity}: a base completa do Brasil tem ~900 mil registros,
 * e as 5 colunas extras de auditoria (uuid + timestamps + author)
 * multiplicariam o volume sem beneficio real — CEPs sao dados de
 * referencia imutaveis, nao registros auditados do ERP.</p>
 *
 * <p>A PK natural e o proprio CEP (8 digitos, sem hifen), o que cria
 * automaticamente o indice primario usado no lookup
 * {@code GET /api/v1/ceps/{cep}}.</p>
 */
@Entity
@Table(name = "ceps", indexes = {
        @Index(name = "idx_ceps_uf_cidade", columnList = "uf, cidade")
})
@Getter
@Setter
@NoArgsConstructor
public class Cep {

    /**
     * CEP em formato limpo: 8 digitos, sem hifen (ex.: {@code "01310100"}).
     * PK natural — o proprio CEP.
     */
    @Id
    @Column(name = "cep", length = 8, updatable = false, nullable = false)
    private String cep;

    /**
     * Logradouro (rua, avenida, etc.). Pode ser nulo quando o CEP e
     * generico da cidade/bairro (sem logradouro especifico).
     */
    @Column(name = "logradouro", length = 200)
    private String logradouro;

    /**
     * Bairro. Opcional (alguns CEPs nao tem bairro associado).
     */
    @Column(name = "bairro", length = 100)
    private String bairro;

    /**
     * Cidade. Obrigatorio.
     */
    @Column(name = "cidade", length = 100, nullable = false)
    private String cidade;

    /**
     * Unidade Federativa (UF) - 2 letras (ex.: {@code "SP"}).
     */
    @Column(name = "uf", length = 2, nullable = false)
    private String uf;

    /**
     * Latitude (decimal). Opcional — presente na base CEP Aberto.
     */
    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    /**
     * Longitude (decimal). Opcional — presente na base CEP Aberto.
     */
    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;
}