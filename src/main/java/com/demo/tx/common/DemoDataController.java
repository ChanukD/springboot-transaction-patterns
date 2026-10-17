package com.demo.tx.common;

import com.demo.tx.events.CustomerOrderRepository;
import com.demo.tx.events.NotificationLog;
import com.demo.tx.idempotency.ProcessedMessageRepository;
import com.demo.tx.outbox.InMemoryBroker;
import com.demo.tx.outbox.OutboxEventRepository;
import com.demo.tx.propagation.AuditLogRepository;
import com.demo.tx.template.PaymentRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/** POST /demo/reset puts everything back to: account #1 "alice" with 1000.00. */
@RestController
@RequestMapping("/demo")
public class DemoDataController implements ApplicationRunner {

    private final AccountRepository accounts;
    private final AuditLogRepository audits;
    private final PaymentRepository payments;
    private final CustomerOrderRepository orders;
    private final OutboxEventRepository outbox;
    private final ProcessedMessageRepository processed;
    private final NotificationLog notifications;
    private final InMemoryBroker broker;

    public DemoDataController(AccountRepository accounts, AuditLogRepository audits, PaymentRepository payments,
                              CustomerOrderRepository orders, OutboxEventRepository outbox,
                              ProcessedMessageRepository processed, NotificationLog notifications,
                              InMemoryBroker broker) {
        this.accounts = accounts;
        this.audits = audits;
        this.payments = payments;
        this.orders = orders;
        this.outbox = outbox;
        this.processed = processed;
        this.notifications = notifications;
        this.broker = broker;
    }

    @Override
    public void run(ApplicationArguments args) {
        reset();
    }

    @PostMapping("/reset")
    @Transactional
    public Account reset() {
        audits.deleteAllInBatch();
        payments.deleteAllInBatch();
        outbox.deleteAllInBatch();
        orders.deleteAllInBatch();
        processed.deleteAllInBatch();
        accounts.deleteAllInBatch();
        notifications.clear();
        broker.clear();
        broker.setDown(false);
        return accounts.save(new Account(DemoRunner.ACCOUNT_ID, "alice", new BigDecimal("1000.00")));
    }

    @GetMapping("/account")
    public Account account() {
        return accounts.findById(DemoRunner.ACCOUNT_ID).orElseThrow();
    }
}
