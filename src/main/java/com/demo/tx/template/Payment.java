package com.demo.tx.template;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.math.BigDecimal;

@Entity
public class Payment {

    public enum Status { PENDING, COMPLETED, FAILED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private Status status;

    private String gatewayReference;

    protected Payment() {
    }

    public static Payment pending(BigDecimal amount) {
        Payment p = new Payment();
        p.amount = amount;
        p.status = Status.PENDING;
        return p;
    }

    public void complete(String reference) {
        this.status = Status.COMPLETED;
        this.gatewayReference = reference;
    }

    public void fail() {
        this.status = Status.FAILED;
    }

    public Long getId() { return id; }
    public BigDecimal getAmount() { return amount; }
    public Status getStatus() { return status; }
    public String getGatewayReference() { return gatewayReference; }
}
