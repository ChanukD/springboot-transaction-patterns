package com.demo.tx.basics;

import com.demo.tx.common.Account;
import com.demo.tx.common.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class BasicsService {

    private static final Logger log = LoggerFactory.getLogger(BasicsService.class);

    private final AccountRepository accounts;
    private final AccountTxOps txOps;

    public BasicsService(AccountRepository accounts, AccountTxOps txOps) {
        this.accounts = accounts;
        this.txOps = txOps;
    }

    // ---------- Trap 1: self-invocation ----------

    /** BROKEN: calls a @Transactional method on "this", so the proxy is skipped. */
    public void checkoutSelfInvocation(Long accountId, BigDecimal amount) {
        this.debitThenFail(accountId, amount);
    }

    @Transactional
    public void debitThenFail(Long accountId, BigDecimal amount) {
        Account account = accounts.findById(accountId).orElseThrow();
        account.debit(amount);
        accounts.save(account);   // no outer tx -> save() commits in its own tx immediately
        throw new IllegalStateException("shipping service down after debit");
    }

    /** FIXED: same logic, but called through another bean's proxy. */
    public void checkoutViaProxy(Long accountId, BigDecimal amount) {
        txOps.debitThenFail(accountId, amount);
    }

    // ---------- Trap 3: inner failure caught by the outer method ----------

    /**
     * The inner REQUIRED call fails and marks the SHARED transaction rollback-only.
     * Catching the exception does not undo that, so commit throws UnexpectedRollbackException.
     */
    @Transactional
    public void checkoutWithOptionalBonus(Long accountId, BigDecimal amount) {
        accounts.findById(accountId).orElseThrow().debit(amount);
        try {
            txOps.applyLoyaltyBonus(accountId);
        } catch (IllegalStateException e) {
            log.warn("Bonus failed, continuing checkout: {}", e.getMessage());
        }
    }
}
