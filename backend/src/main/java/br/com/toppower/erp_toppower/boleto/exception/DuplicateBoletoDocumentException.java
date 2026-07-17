package br.com.toppower.erp_toppower.boleto.exception;

public class DuplicateBoletoDocumentException extends RuntimeException {

    public DuplicateBoletoDocumentException(String documentNumber) {
        super("Já existe um boleto cadastrado com o número de documento: " + documentNumber);
    }
}