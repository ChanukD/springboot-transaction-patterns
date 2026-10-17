package com.demo.tx.events;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Stand-in for an email/SMS provider: records what would have been sent. */
@Component
public class NotificationLog {

    private final List<String> sent = new CopyOnWriteArrayList<>();

    public void send(String message) {
        sent.add(message);
    }

    public List<String> all() {
        return List.copyOf(sent);
    }

    public void clear() {
        sent.clear();
    }
}
