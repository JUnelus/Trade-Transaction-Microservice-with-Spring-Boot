package com.dtcc.trade.exception;

public class TradeNotFoundException extends RuntimeException {

    public TradeNotFoundException(Long id) {
        super("Trade with id " + id + " was not found");
    }
}

