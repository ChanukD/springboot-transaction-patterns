package com.demo.tx.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.Instant;
import java.util.UUID;

@Entity
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique id consumers use for de-duplication. */
    @Column(nullable = false, unique = true)
    private String eventId;

    private String topic;
    private String messageKey;

    @Column(length = 4000)
    private String payload;

    private Instant createdAt;
    private Instant publishedAt;
    private int attempts;

    protected OutboxEvent() {
    }

    public OutboxEvent(String topic, String messageKey, String payload) {
        this.eventId = UUID.randomUUID().toString();
        this.topic = topic;
        this.messageKey = messageKey;
        this.payload = payload;
        this.createdAt = Instant.now();
    }

    public void markPublished() {
        this.publishedAt = Instant.now();
    }

    public void recordAttempt() {
        this.attempts++;
    }

    public Long getId() { return id; }
    public String getEventId() { return eventId; }
    public String getTopic() { return topic; }
    public String getMessageKey() { return messageKey; }
    public String getPayload() { return payload; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getPublishedAt() { return publishedAt; }
    public int getAttempts() { return attempts; }
}
