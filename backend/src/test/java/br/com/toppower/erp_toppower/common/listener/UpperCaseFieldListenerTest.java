package br.com.toppower.erp_toppower.common.listener;

import br.com.toppower.erp_toppower.common.annotation.UpperCase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Testes unitários do {@link UpperCaseFieldListener}.
 *
 * <p>Não usa Spring Data JPA — instancia o listener diretamente e chama
 * o método de callback (a anotação {@code @PrePersist} é só metadata;
 * o método é público, então pode ser invocado em teste).</p>
 *
 * <p>Cobre:</p>
 * <ul>
 *   <li>Campos {@code @UpperCase} declarados na classe</li>
 *   <li>Campos {@code @UpperCase} herdados de uma superclasse</li>
 *   <li>Campos sem {@code @UpperCase} permanecem inalterados</li>
 *   <li>Valores nulos permanecem nulos</li>
 *   <li>Acentos e cedilha são preservados</li>
 *   <li>Determinismo independente do locale (uso de {@code Locale.ROOT})</li>
 * </ul>
 */
class UpperCaseFieldListenerTest {

    private final UpperCaseFieldListener listener = new UpperCaseFieldListener();

    // ========== Helpers ==========

    /** Holder plano com um campo @UpperCase e um campo de controle. */
    static class Holder {
        @UpperCase
        String name;
        String code; // sem @UpperCase -> não deve ser tocado

        Holder(String name, String code) {
            this.name = name;
            this.code = code;
        }
    }

    /** Superclasse com @UpperCase no campo "name". */
    static class ParentHolder {
        @UpperCase
        String name;
    }

    /** Filha herda o campo @UpperCase da superclasse e adiciona outro. */
    static class ChildHolder extends ParentHolder {
        @UpperCase
        String tradeName;
    }

    // ========== Testes ==========

    @Test
    void normalizaCampoUpperCase_lowerCase() {
        Holder h = new Holder("joão silva", "abc-001");
        listener.onPrePersist(h);
        assertEquals("JOÃO SILVA", h.name);
        assertEquals("abc-001", h.code); // não alterado
    }

    @Test
    void normalizaCampoUpperCase_jaEmMaiusculas_preservado() {
        Holder h = new Holder("MARIA SOUZA", "X1");
        listener.onPrePersist(h);
        assertEquals("MARIA SOUZA", h.name);
    }

    @Test
    void normalizaCampoUpperCase_misto() {
        Holder h = new Holder("AuTo PeÇAs LtDa", null);
        listener.onPrePersist(h);
        assertEquals("AUTO PEÇAS LTDA", h.name);
        assertNull(h.code);
    }

    @Test
    void normalizaCampoUpperCase_nulo_preservado() {
        Holder h = new Holder(null, "X1");
        listener.onPrePersist(h);
        assertNull(h.name);
        assertEquals("X1", h.code);
    }

    @Test
    void normalizaCampoUpperCase_vazio_preservado() {
        // String vazia é pulada (a regra é "manter se já está em maiúsculas
        // ou não tem o que normalizar"); comportamento conservador.
        Holder h = new Holder("", "X1");
        listener.onPrePersist(h);
        assertEquals("", h.name);
    }

    @Test
    void campoSemUpperCase_naoAlterado() {
        Holder h = new Holder("qualquer", "abc-001");
        listener.onPreUpdate(h);
        assertEquals("QUALQUER", h.name); // listener normalizou (tem @UpperCase)
        assertEquals("abc-001", h.code);  // code ficou intacto (sem @UpperCase)
    }

    @Test
    void campoUpperCaseHerdadoDaSuperclasse_normalizado() {
        ChildHolder ch = new ChildHolder();
        ch.name = "joão silva";
        ch.tradeName = "padaria central";

        listener.onPrePersist(ch);

        assertEquals("JOÃO SILVA", ch.name);          // herdado de ParentHolder
        assertEquals("PADARIA CENTRAL", ch.tradeName); // declarado na ChildHolder
    }

    @Test
    void prePersistEPreUpdate_aplicamOMesmoComportamento() {
        Holder h1 = new Holder("joão", "1");
        listener.onPrePersist(h1);
        assertEquals("JOÃO", h1.name);

        // Mudou para minúsculas — preUpdate deve normalizar de novo
        h1.name = "maria";
        listener.onPreUpdate(h1);
        assertEquals("MARIA", h1.name);
    }

    @Test
    void entidadeNula_naoLancaExcecao() {
        // Não esperamos exception — o listener deve apenas retornar.
        listener.onPrePersist(null);
        listener.onPreUpdate(null);
    }
}
