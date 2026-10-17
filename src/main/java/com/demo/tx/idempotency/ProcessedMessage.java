package com.demo.tx.idempotency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.Instant;

/** One row per message we have handled. The UNIQUE constraint is the real guard. */
@Entity
public class ProcessedMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String eventId;

    private Instant processedAt;

    protected ProcessedMessage() {
    }

    public ProcessedMessage(String eventId) {
        this.eventId = eventId;
        this.processedAt = Instant.now();
    }

    public String getEventId() { return eventId; }
    public Instant getProcessedAt() { return processedAt; }
}
