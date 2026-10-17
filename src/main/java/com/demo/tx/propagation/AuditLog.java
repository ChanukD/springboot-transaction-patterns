package com.demo.tx.propagation;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.Instant;

@Entity
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String propagation;
    private String message;
    private Instant createdAt;

    protected AuditLog() {
    }

    public AuditLog(String propagation, String message) {
        this.propagation = propagation;
        this.message = message;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getPropagation() { return propagation; }
    public String getMessage() { return message; }
    public Instant getCreatedAt() { return createdAt; }
}
