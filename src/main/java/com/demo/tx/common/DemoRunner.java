package com.demo.tx.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Runs a scenario and captures the account balance before and after,
 * so you can see whether the work was committed or rolled back.
 */
@Component
public class DemoRunner {

    public static final long ACCOUNT_ID = 1L;
    private static final Logger log = LoggerFactory.getLogger(DemoRunner.class);

    private final AccountRepository accounts;

    public DemoRunner(AccountRepository accounts) {
        this.accounts = accounts;
    }

    @FunctionalInterface
    public interface Action {
        void run() throws Exception;
    }

    public DemoResult run(String scenario, String lesson, Action action) {
        log.info("===== {} =====", scenario);
        BigDecimal before = balance();
        String outcome;
        try {
            action.run();
            outcome = "completed normally";
        } catch (Exception e) {
            outcome = "threw " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
        BigDecimal after = balance();
        log.info("===== balance {} -> {} =====", before, after);
        return new DemoResult(scenario, outcome, before, after, lesson);
    }

    public BigDecimal balance() {
        return accounts.findById(ACCOUNT_ID).map(Account::getBalance).orElseThrow();
    }
}
