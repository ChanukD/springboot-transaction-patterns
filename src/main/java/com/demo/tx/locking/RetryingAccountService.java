package com.demo.tx.locking;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Retry lives in a DIFFERENT bean from @Transactional, so every retry
 * starts a brand-new transaction and reads the latest version.
 * Spring Framework 7 / Boot 4 built-in @Retryable (needs @EnableResilientMethods).
 * On Boot 3, use spring-retry's @Retryable(retryFor = ..., maxAttempts = ...) instead.
 */
@Service
public class RetryingAccountService {

    private final LockingTxService txService;

    public RetryingAccountService(LockingTxService txService) {
        this.txService = txService;
    }

    @Retryable(includes = ObjectOptimisticLockingFailureException.class,
            maxRetries = 10, delay = 20, multiplier = 2, maxDelay = 500, jitter = 10)
    public void debit(Long accountId, BigDecimal amount) {
        txService.debitOptimistic(accountId, amount);
    }
}
