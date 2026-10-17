package com.demo.tx.common;

import java.math.BigDecimal;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(Long accountId, BigDecimal balance, BigDecimal amount) {
        super("Account %d has %s, cannot debit %s".formatted(accountId, balance, amount));
    }
}
