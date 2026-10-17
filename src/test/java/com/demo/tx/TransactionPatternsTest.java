package com.demo.tx;

import com.demo.tx.basics.BasicsController;
import com.demo.tx.common.DemoDataController;
import com.demo.tx.common.DemoResult;
import com.demo.tx.events.EventsController;
import com.demo.tx.idempotency.IdempotencyController;
import com.demo.tx.locking.LockingController;
import com.demo.tx.outbox.OutboxController;
import com.demo.tx.propagation.AuditLog;
import com.demo.tx.propagation.PropagationController;
import com.demo.tx.readonly.ReadOnlyController;
import com.demo.tx.template.Payment;
import com.demo.tx.template.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class TransactionPatternsTest {

    @Autowired DemoDataController data;
    @Autowired BasicsController basics;
    @Autowired PropagationController propagation;
    @Autowired ReadOnlyController readOnly;
    @Autowired PaymentService payments;
    @Autowired LockingController locking;
    @Autowired EventsController events;
    @Autowired OutboxController outbox;
    @Autowired IdempotencyController idempotency;

    @BeforeEach
    void reset() {
        data.reset();
    }

    private static BigDecimal money(String v) {
        return new BigDecimal(v);
    }

    @Nested
    class ProxyTraps {

        @Test
        void selfInvocationIgnoresTransactional() {
            DemoResult r = basics.selfInvocation();
            assertThat(r.balanceAfter()).isEqualByComparingTo("900.00");   // money gone
        }

        @Test
        void callingThroughAnotherBeanRollsBack() {
            DemoResult r = basics.selfInvocationFixed();
            assertThat(r.outcome()).contains("IllegalStateException");
            assertThat(r.balanceAfter()).isEqualByComparingTo("1000.00");
        }

        @Test
        void checkedExceptionCommitsByDefault() {
            assertThat(basics.checkedException().balanceAfter()).isEqualByComparingTo("900.00");
        }

        @Test
        void rollbackForRollsBackCheckedException() {
            assertThat(basics.checkedExceptionFixed().balanceAfter()).isEqualByComparingTo("1000.00");
        }

        @Test
        void catchingInnerFailureStillRollsBackEverything() {
            DemoResult r = basics.unexpectedRollback();
            assertThat(r.outcome()).contains("UnexpectedRollbackException");
            assertThat(r.balanceAfter()).isEqualByComparingTo("1000.00");
        }
    }

    @Nested
    class Propagation {

        @Test
        void requiresNewAuditSurvivesRollback() {
            propagation.withdraw(money("999999"));
            List<AuditLog> audit = propagation.audit();
            assertThat(audit).extracting(AuditLog::getPropagation).containsExactly("REQUIRES_NEW");
        }

        @Test
        void bothAuditRowsSavedOnSuccess() {
            propagation.withdraw(money("100"));
            assertThat(propagation.audit()).extracting(AuditLog::getPropagation)
                    .containsExactlyInAnyOrder("REQUIRES_NEW", "REQUIRED");
        }
    }

    @Nested
    class ReadOnly {

        @Test
        void changesInReadOnlyTransactionAreNotSaved() {
            Map<String, String> r = readOnly.modifyInReadOnly();
            assertThat(r.get("valueInsideMethod")).isEqualTo("ALICE");
            assertThat(r.get("valueInDatabase")).isEqualTo("alice");
        }

        @Test
        void dirtyCheckingSavesInReadWriteTransaction() {
            assertThat(readOnly.rename("bob").get("valueInDatabase")).isEqualTo("bob");
        }
    }

    @Nested
    class TransactionTemplateSplit {

        @Test
        void remoteCallInsideTransactionHoldsConnection() {
            assertThat(payments.payInsideTransaction(money("50"), false).dbTransactionOpenDuringGatewayCall()).isTrue();
        }

        @Test
        void splitTransactionsKeepRemoteCallOutsideAndRecordFailures() {
            PaymentService.PayResult ok = payments.pay(money("70"), false);
            assertThat(ok.dbTransactionOpenDuringGatewayCall()).isFalse();
            assertThat(ok.payment().getStatus()).isEqualTo(Payment.Status.COMPLETED);

            PaymentService.PayResult declined = payments.pay(money("80"), true);
            assertThat(declined.payment().getStatus()).isEqualTo(Payment.Status.FAILED);   // attempt is recorded
        }
    }

    @Nested
    class Locking {

        @Test
        void optimisticWithoutRetryRejectsConcurrentUpdates() throws Exception {
            var r = locking.optimisticNoRetry(8);
            assertThat(r.failed()).isPositive();
            assertThat(r.errors()).anyMatch(e -> e.contains("ObjectOptimisticLockingFailureException"));
            // no lost update: balance reflects exactly the successful debits
            assertThat(r.balanceAfter()).isEqualByComparingTo(
                    r.balanceBefore().subtract(money("10").multiply(BigDecimal.valueOf(r.succeeded()))));
        }

        @Test
        void optimisticWithRetryEventuallyAppliesAll() throws Exception {
            var r = locking.optimisticRetry(8);
            assertThat(r.succeeded()).isEqualTo(8);
            assertThat(r.totalAttempts()).isGreaterThan(8);
            assertThat(r.balanceAfter()).isEqualByComparingTo("920.00");
        }

        @Test
        void pessimisticSerializesUpdates() throws Exception {
            var r = locking.pessimistic(8);
            assertThat(r.succeeded()).isEqualTo(8);
            assertThat(r.balanceAfter()).isEqualByComparingTo("920.00");
        }
    }

    @Nested
    class TransactionalEvents {

        @Test
        void afterCommitListenerRunsOnlyOnCommit() {
            Map<String, Object> r = events.placeOrder("camera", true);
            @SuppressWarnings("unchecked")
            List<String> sent = (List<String>) r.get("notificationsSent");
            assertThat(sent).anyMatch(s -> s.startsWith("[@EventListener]"));    // the bug: email for a rolled-back order
            assertThat(sent).noneMatch(s -> s.startsWith("[AFTER_COMMIT]"));
            assertThat(sent).anyMatch(s -> s.startsWith("[AFTER_ROLLBACK]"));
            assertThat(r.get("ordersInDatabase")).isEqualTo(0L);
        }

        @Test
        void afterCommitListenerRunsOnSuccess() {
            @SuppressWarnings("unchecked")
            List<String> sent = (List<String>) events.placeOrder("laptop", false).get("notificationsSent");
            assertThat(sent).anyMatch(s -> s.startsWith("[AFTER_COMMIT]"));
        }
    }

    @Nested
    class Outbox {

        @Test
        void orderAndEventRollBackTogether() {
            Map<String, Object> r = outbox.placeOrder("phone", true);
            assertThat(r.get("ordersInDatabase")).isEqualTo(0L);
            assertThat(r.get("outboxRows")).isEqualTo(0L);
        }

        @Test
        void eventsWaitDuringBrokerOutageThenGetDelivered() {
            outbox.broker(true);
            outbox.placeOrder("phone", false);
            outbox.placeOrder("tablet", false);

            assertThat(outbox.relayNow().get("publishedNow")).isEqualTo(0);
            assertThat(outbox.state().get("outboxPending")).isEqualTo(2L);

            outbox.broker(false);
            assertThat(outbox.relayNow().get("publishedNow")).isEqualTo(2);
            assertThat(outbox.state().get("outboxPending")).isEqualTo(0L);
        }
    }

    @Nested
    class Idempotency {

        @Test
        void naiveConsumerAppliesDuplicates() throws Exception {
            Map<String, Object> r = idempotency.naive("pay-001", money("100"), 3, false);
            assertThat((BigDecimal) r.get("balanceAfter")).isEqualByComparingTo("1300.00");   // credited 3 times
        }

        @Test
        void safeConsumerAppliesOnceSequential() throws Exception {
            Map<String, Object> r = idempotency.safe("pay-002", money("100"), 3, false);
            assertThat((BigDecimal) r.get("balanceAfter")).isEqualByComparingTo("1100.00");
        }

        @Test
        void safeConsumerAppliesOnceConcurrent() throws Exception {
            Map<String, Object> r = idempotency.safe("pay-003", money("100"), 5, true);
            assertThat((BigDecimal) r.get("balanceAfter")).isEqualByComparingTo("1100.00");
            @SuppressWarnings("unchecked")
            List<String> outcomes = (List<String>) r.get("outcomes");
            assertThat(outcomes).containsOnlyOnce("APPLIED");
            assertThat(outcomes).allMatch(o -> o.equals("APPLIED") || o.equals("DUPLICATE_SKIPPED"));
        }
    }
}
