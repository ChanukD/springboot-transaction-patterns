package com.demo.tx.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Fake Kafka. Can be switched "down" to show the outbox surviving a broker outage. */
@Component
public class InMemoryBroker implements MessagePublisher {

    public record Message(String topic, String key, String eventId, String payload) {
    }

    private static final Logger log = LoggerFactory.getLogger(InMemoryBroker.class);

    private final List<Message> delivered = new CopyOnWriteArrayList<>();
    private volatile boolean down;

    @Override
    public void publish(String topic, String key, String eventId, String payload) {
        if (down) {
            throw new IllegalStateException("broker unavailable");
        }
        delivered.add(new Message(topic, key, eventId, payload));
        log.info("Published {} to {} (key={})", eventId, topic, key);
    }

    public List<Message> delivered() { return List.copyOf(delivered); }
    public boolean isDown() { return down; }
    public void setDown(boolean down) { this.down = down; }
    public void clear() { delivered.clear(); }
}
