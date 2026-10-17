package com.demo.tx.template;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;

@Service
public class PaymentService {

    public record PayResult(Payment payment, boolean dbTransactionOpenDuringGatewayCall) {
    }

    private final PaymentRepository payments;
    private final FakePaymentGateway gateway;
    private final TransactionTemplate tx;

    public PaymentService(PaymentRepository payments, FakePaymentGateway gateway, TransactionTemplate tx) {
        this.payments = payments;
        this.gateway = gateway;
        this.tx = tx;
    }

    /**
     * BAD: the 1.5s gateway call runs inside the DB transaction.
     * The connection and row locks are held the whole time. If the gateway fails,
     * the PENDING row is rolled back, so there is no record the charge was attempted.
     */
    @Transactional
    public PayResult payInsideTransaction(BigDecimal amount, boolean decline) {
        Payment payment = payments.save(Payment.pending(amount));
        FakePaymentGateway.ChargeResult result = gateway.charge(amount, decline);
        payment.complete(result.reference());
        return new PayResult(payment, result.dbTransactionOpenDuringCall());
    }

    /** GOOD: two short transactions with the remote call in between. */
    public PayResult pay(BigDecimal amount, boolean decline) {
        // tx 1: record the attempt. Committed before we talk to the gateway.
        Long paymentId = tx.execute(status -> payments.save(Payment.pending(amount)).getId());

        FakePaymentGateway.ChargeResult result;
        try {
            result = gateway.charge(amount, decline);            // no transaction, no connection held
        } catch (RuntimeException e) {
            tx.executeWithoutResult(status -> payments.findById(paymentId).orElseThrow().fail());
            return new PayResult(payments.findById(paymentId).orElseThrow(), false);
        }

        // tx 2: record the result.
        tx.executeWithoutResult(status -> payments.findById(paymentId).orElseThrow().complete(result.reference()));
        return new PayResult(payments.findById(paymentId).orElseThrow(), result.dbTransactionOpenDuringCall());
    }
}
