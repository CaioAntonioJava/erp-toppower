package br.com.toppower.erp_toppower.common.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Componente reutilizável que representa um endereço.
 *
 * <p>Marcado com {@link Embeddable} para ser embutido em outras entidades
 * (ex: {@code Company}, {@code Customer}, {@code Supplier}, {@code User})
 * sem precisar de uma tabela própria.</p>
 *
 * <p>O {@code @AttributeOverrides} é recomendado quando os nomes das colunas
 * do endereço podem colidir com colunas da entidade dona (ex: ambos terem
 * um campo "city"). O prefixo "address_" evita ambiguidade.</p>
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class Address {

    /**
     * Logradouro (rua, avenida, etc.).
     * Obrigatório.
     */
    @Column(name = "street", nullable = false, length = 200)
    private String street;

    /**
     * Número do imóvel.
     * String (não int) para aceitar casos como "S/N" (sem número).
     * Obrigatório.
     */
    @Column(name = "number", nullable = false, length = 20)
    private String number;

    /**
     * Complemento (apto, bloco, sala, etc.).
     * Opcional.
     */
    @Column(name = "complement", length = 100)
    private String complement;

    /**
     * Bairro.
     * Opcional.
     */
    @Column(name = "neighborhood", length = 100)
    private String neighborhood;

    /**
     * Cidade.
     * Obrigatório.
     */
    @Column(name = "city", nullable = false, length = 100)
    private String city;

    /**
     * Unidade Federativa (UF) - 2 letras (ex: "SP", "RJ", "MG").
     * Obrigatório.
     */
    @Column(name = "state", nullable = false, length = 2)
    private String state;

    /**
     * CEP (Código de Endereçamento Postal).
     * 8 dígitos (formato: "12345678" ou "12345-678").
     * Obrigatório.
     */
    @Column(name = "zip_code", nullable = false, length = 9)
    private String zipCode;
}
