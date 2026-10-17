package com.demo.tx.basics;

/** A CHECKED exception. Spring does not roll back on these by default. */
public class PaymentDeclinedException extends Exception {
    public PaymentDeclinedException(String message) {
        super(message);
    }
}
