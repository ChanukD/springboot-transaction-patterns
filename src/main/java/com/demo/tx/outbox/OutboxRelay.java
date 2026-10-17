package com.demo.tx.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Moves events from the outbox table to the broker. Delivery is AT-LEAST-ONCE:
 * if we crash after publish() but before commit, the event is sent again next time.
 * That is why consumers must be idempotent (see the idempotency demo).
 */
@Service
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private static final int BATCH_SIZE = 50;

    private final OutboxEventRepository outbox;
    private final MessagePublisher publisher;

    public OutboxRelay(OutboxEventRepository outbox, MessagePublisher publisher) {
        this.outbox = outbox;
        this.publisher = publisher;
    }

    /** Returns how many events were published. Stops at the first failure, keeps what succeeded. */
    @Transactional(timeout = 30)
    public int relayBatch() {
        List<OutboxEvent> batch = outbox.lockNextBatch(BATCH_SIZE);
        int published = 0;
        for (OutboxEvent event : batch) {
            event.recordAttempt();
            try {
                publisher.publish(event.getTopic(), event.getMessageKey(), event.getEventId(), event.getPayload());
                event.markPublished();
                published++;
            } catch (RuntimeException e) {
                log.warn("Publish failed for {}, will retry next poll: {}", event.getEventId(), e.getMessage());
                break;   // keep order per key; remaining rows stay unpublished
            }
        }
        return published;
    }
}
