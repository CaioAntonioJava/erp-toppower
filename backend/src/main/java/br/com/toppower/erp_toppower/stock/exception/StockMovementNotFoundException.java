package br.com.toppower.erp_toppower.stock.exception;

public class StockMovementNotFoundException extends RuntimeException {

    public StockMovementNotFoundException(Long id) {
        super("Movimentação de estoque não encontrada: " + id);
    }
}