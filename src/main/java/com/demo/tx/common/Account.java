package com.demo.tx.common;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

import java.math.BigDecimal;

@Entity
public class Account {

    @Id
    private Long id;

    private String owner;

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal balance;

    /** Optimistic lock. Hibernate adds "where version = ?" to every update. */
    @Version
    private Long version;

    protected Account() {
    }

    public Account(Long id, String owner, BigDecimal balance) {
        this.id = id;
        this.owner = owner;
        this.balance = balance;
    }

    public void debit(BigDecimal amount) {
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException(id, balance, amount);
        }
        balance = balance.subtract(amount);
    }

    public void credit(BigDecimal amount) {
        balance = balance.add(amount);
    }

    public Long getId() { return id; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public BigDecimal getBalance() { return balance; }
    public Long getVersion() { return version; }
}
