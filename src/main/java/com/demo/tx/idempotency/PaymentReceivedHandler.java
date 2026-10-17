package com.demo.tx.idempotency;

import com.demo.tx.common.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentReceivedHandler {

    private final AccountRepository accounts;
    private final ProcessedMessageRepository processed;

    public PaymentReceivedHandler(AccountRepository accounts, ProcessedMessageRepository processed) {
        this.accounts = accounts;
        this.processed = processed;
    }

    /** NAIVE: every delivery credits the account again. */
    @Transactional
    public void applyNaive(PaymentReceived event) {
        accounts.findById(event.accountId()).orElseThrow().credit(event.amount());
    }

    /**
     * SAFE: insert the event id FIRST, in the same transaction as the business change.
     * saveAndFlush sends the INSERT now, so a duplicate fails before we touch the balance.
     * A duplicate rolls back the whole transaction: nothing is applied twice.
     */
    @Transactional
    public void applyOnce(PaymentReceived event) {
        processed.saveAndFlush(new ProcessedMessage(event.eventId()));
        accounts.findById(event.accountId()).orElseThrow().credit(event.amount());
    }
}
