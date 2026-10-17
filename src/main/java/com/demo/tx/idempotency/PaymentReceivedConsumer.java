package com.demo.tx.idempotency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;

/**
 * Entry point, like a @KafkaListener or an SQS/webhook handler.
 * Deliberately NOT @Transactional: it must catch the duplicate AFTER the transaction rolled back.
 */
@Component
public class PaymentReceivedConsumer {

    public enum Outcome { APPLIED, DUPLICATE_SKIPPED }

    private static final Logger log = LoggerFactory.getLogger(PaymentReceivedConsumer.class);

    private final PaymentReceivedHandler handler;

    public PaymentReceivedConsumer(PaymentReceivedHandler handler) {
        this.handler = handler;
    }

    /**
     * Two copies arriving at the same moment can collide on the unique index with a lock error
     * instead of a duplicate-key error (depends on the DB). Retrying turns that into a clean duplicate.
     */
    @Retryable(includes = PessimisticLockingFailureException.class, maxRetries = 5, delay = 50, jitter = 25)
    public Outcome onMessage(PaymentReceived event) {
        try {
            handler.applyOnce(event);
            return Outcome.APPLIED;
        } catch (DataIntegrityViolationException duplicate) {
            log.info("Duplicate delivery of {} ignored", event.eventId());
            return Outcome.DUPLICATE_SKIPPED;   // ack the message; it was already handled
        }
    }
}
