package br.com.toppower.erp_toppower.stock.exception;

import java.math.BigDecimal;

/**
 * Lançada quando uma operação de saída de estoque não pode ser concluída
 * por insuficiência de saldo do produto. Carrega os dados do produto e os
 * valores envolvidos para que o handler global produza uma resposta de
 * erro informativa (HTTP 422).
 */
public class InsufficientStockException extends RuntimeException {

    private final String productName;
    private final String productCode;
    private final BigDecimal available;
    private final BigDecimal requested;

    public InsufficientStockException(String productName, String productCode,
                                      BigDecimal available, BigDecimal requested) {
        super(buildMessage(productName, productCode, available, requested));
        this.productName = productName;
        this.productCode = productCode;
        this.available = available;
        this.requested = requested;
    }

    public String getProductName() {
        return productName;
    }

    public String getProductCode() {
        return productCode;
    }

    public BigDecimal getAvailable() {
        return available;
    }

    public BigDecimal getRequested() {
        return requested;
    }

    private static String buildMessage(String productName, String productCode,
                                       BigDecimal available, BigDecimal requested) {
        String ident = (productCode != null && !productCode.isBlank())
                ? productName + " (SKU " + productCode + ")"
                : productName;
        return "Estoque insuficiente para " + ident
                + ": disponível " + available.toPlainString()
                + ", solicitado " + requested.toPlainString() + ".";
    }
}