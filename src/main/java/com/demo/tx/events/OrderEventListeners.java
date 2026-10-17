package com.demo.tx.events;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderEventListeners {

    private final NotificationLog notifications;

    public OrderEventListeners(NotificationLog notifications) {
        this.notifications = notifications;
    }

    /** BAD for side effects: runs immediately, BEFORE we know if the order commits. */
    @EventListener
    public void sendEmailImmediately(OrderPlaced event) {
        notifications.send("[@EventListener] email: order #%d (%s) confirmed".formatted(event.orderId(), event.item()));
    }

    /** GOOD: runs only after the transaction has committed. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendEmailAfterCommit(OrderPlaced event) {
        notifications.send("[AFTER_COMMIT] email: order #%d (%s) confirmed".formatted(event.orderId(), event.item()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void onRollback(OrderPlaced event) {
        notifications.send("[AFTER_ROLLBACK] order #%d (%s) rolled back, no email".formatted(event.orderId(), event.item()));
    }
}
