package com.demo.tx.locking;

import com.demo.tx.common.Account;
import com.demo.tx.common.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class LockingTxService {

    private final AccountRepository accounts;
    private final AtomicInteger optimisticAttempts = new AtomicInteger();

    public LockingTxService(AccountRepository accounts) {
        this.accounts = accounts;
    }

    // Accessed through methods, never fields: callers hold the CGLIB proxy, whose fields are null.
    public int attempts() { return optimisticAttempts.get(); }
    public void resetAttempts() { optimisticAttempts.set(0); }

    /** Reads the row without a lock. On commit: UPDATE ... WHERE id=? AND version=? */
    @Transactional
    public void debitOptimistic(Long accountId, BigDecimal amount) {
        optimisticAttempts.incrementAndGet();
        Account account = accounts.findById(accountId).orElseThrow();
        simulateWork();   // widens the race window so conflicts are easy to see
        account.debit(amount);
    }

    /** SELECT ... FOR UPDATE: other transactions wait here until we commit. */
    @Transactional(timeout = 10)
    public void debitPessimistic(Long accountId, BigDecimal amount) {
        Account account = accounts.findForUpdate(accountId).orElseThrow();
        simulateWork();
        account.debit(amount);
    }

    private static void simulateWork() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
