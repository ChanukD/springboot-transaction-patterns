package com.demo.tx.outbox;

/**
 * Port to the message broker. In a real service this wraps KafkaTemplate:
 *   kafkaTemplate.send(topic, key, payload).get(5, SECONDS);
 * Sending synchronously matters: if the broker rejects it, we must NOT mark the row as published.
 */
public interface MessagePublisher {
    void publish(String topic, String key, String eventId, String payload);
}
