package com.demo.tx.common;

import java.math.BigDecimal;

/** What every demo endpoint returns, so the effect of the transaction is visible. */
public record DemoResult(
        String scenario,
        String outcome,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        String lesson) {
}
