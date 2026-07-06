package br.com.toppower.erp_toppower.stock.exception;

import java.util.UUID;

public class StockMovementNotFoundException extends RuntimeException {

    public StockMovementNotFoundException(UUID uuid) {
        super("Movimentação de estoque não encontrada: " + uuid);
    }
}