package com.demo.tx.readonly;

import com.demo.tx.common.Account;
import com.demo.tx.common.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Class default: read-only. Write methods opt out explicitly. */
@Service
@Transactional(readOnly = true)
public class ProfileService {

    private final AccountRepository accounts;

    public ProfileService(AccountRepository accounts) {
        this.accounts = accounts;
    }

    /** Bug on purpose: a "read" method that modifies a managed entity. readOnly stops the change. */
    public String displayNameInReadOnly(Long accountId) {
        Account account = accounts.findById(accountId).orElseThrow();
        account.setOwner(account.getOwner().toUpperCase());   // no flush, no dirty check -> not saved
        return account.getOwner();
    }

    @Transactional
    public String rename(Long accountId, String owner) {
        accounts.findById(accountId).orElseThrow().setOwner(owner);   // dirty checking saves it, no save() call
        return owner;
    }

    public String owner(Long accountId) {
        return accounts.findById(accountId).orElseThrow().getOwner();
    }
}
