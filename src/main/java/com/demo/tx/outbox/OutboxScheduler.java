package com.demo.tx.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Polls the outbox. With several pods, SKIP LOCKED already prevents double work;
 * add ShedLock if you want only one pod polling at a time.
 */
@Component
@ConditionalOnProperty(name = "demo.outbox.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxScheduler.class);

    private final OutboxRelay relay;

    public OutboxScheduler(OutboxRelay relay) {
        this.relay = relay;
    }

    @Scheduled(fixedDelayString = "${demo.outbox.poll-ms:3000}")
    public void poll() {
        int published = relay.relayBatch();
        if (published > 0) {
            log.info("Outbox relay published {} event(s)", published);
        }
    }
}
