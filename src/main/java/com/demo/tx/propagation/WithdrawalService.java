package com.demo.tx.propagation;

import com.demo.tx.common.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class WithdrawalService {

    private final AccountRepository accounts;
    private final AuditService audit;

    public WithdrawalService(AccountRepository accounts, AuditService audit) {
        this.accounts = accounts;
        this.audit = audit;
    }

    /** Use case: compliance requires a record of every withdrawal ATTEMPT, including failed ones. */
    @Transactional
    public void withdraw(Long accountId, BigDecimal amount) {
        String msg = "withdraw %s from account %d".formatted(amount, accountId);
        audit.recordIndependently("attempt: " + msg);
        audit.recordInCallerTx("attempt: " + msg);

        accounts.findById(accountId).orElseThrow().debit(amount);   // throws if balance too low
    }
}
