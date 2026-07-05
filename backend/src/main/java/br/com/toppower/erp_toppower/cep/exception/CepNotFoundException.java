package br.com.toppower.erp_toppower.cep.exception;

public class CepNotFoundException extends RuntimeException {

    public CepNotFoundException(String cep) {
        super("CEP não encontrado na base local: " + cep
                + ". Verifique se a base de CEPs foi carregada via POST /api/v1/ceps/import.");
    }
}