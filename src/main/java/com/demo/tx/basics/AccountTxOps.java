package com.demo.tx.basics;

import com.demo.tx.common.Account;
import com.demo.tx.common.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Transactional building blocks. Living in their own bean means every call
 * from outside goes through the Spring proxy, so @Transactional applies.
 */
@Service
public class AccountTxOps {

    private final AccountRepository accounts;

    public AccountTxOps(AccountRepository accounts) {
        this.accounts = accounts;
    }

    @Transactional
    public void debitThenFail(Long accountId, BigDecimal amount) {
        Account account = accounts.findById(accountId).orElseThrow();
        account.debit(amount);
        accounts.save(account);
        throw new IllegalStateException("shipping service down after debit");
    }

    /** Default rules: checked exception -> COMMIT. Money is gone, payment was declined. */
    @Transactional
    public void debitThenDecline(Long accountId, BigDecimal amount) throws PaymentDeclinedException {
        accounts.findById(accountId).orElseThrow().debit(amount);
        throw new PaymentDeclinedException("card declined");
    }

    /** Fixed: roll back for checked exceptions too. */
    @Transactional(rollbackFor = Exception.class)
    public void debitThenDeclineWithRollbackFor(Long accountId, BigDecimal amount) throws PaymentDeclinedException {
        accounts.findById(accountId).orElseThrow().debit(amount);
        throw new PaymentDeclinedException("card declined");
    }

    /** Joins the caller's transaction (REQUIRED) and marks it rollback-only when it throws. */
    @Transactional
    public void applyLoyaltyBonus(Long accountId) {
        throw new IllegalStateException("loyalty service rejected the bonus");
    }
}
