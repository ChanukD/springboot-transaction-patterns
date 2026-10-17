package com.demo.tx.idempotency;

import java.math.BigDecimal;

/** The message a payment provider (or another service) sends us. eventId is stable across redeliveries. */
public record PaymentReceived(String eventId, Long accountId, BigDecimal amount) {
}
