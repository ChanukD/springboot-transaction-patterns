package com.demo.tx.template;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.UUID;

/** Stand-in for Stripe/Adyen/a bank API: slow, and it can fail. */
@Component
public class FakePaymentGateway {

    public record ChargeResult(String reference, boolean dbTransactionOpenDuringCall) {
    }

    public ChargeResult charge(BigDecimal amount, boolean decline) {
        // Proof for the demo: was a DB transaction (and its connection) held during this call?
        boolean txOpen = TransactionSynchronizationManager.isActualTransactionActive();
        sleep(1500);
        if (decline) {
            throw new IllegalStateException("gateway declined charge of " + amount);
        }
        return new ChargeResult("ch_" + UUID.randomUUID().toString().substring(0, 8), txOpen);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
