package com.demo.tx.events;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.Instant;

@Entity
public class CustomerOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String item;
    private Instant createdAt;

    protected CustomerOrder() {
    }

    public CustomerOrder(String item) {
        this.item = item;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getItem() { return item; }
    public Instant getCreatedAt() { return createdAt; }
}
