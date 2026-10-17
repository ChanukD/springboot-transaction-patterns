package com.demo.tx.basics;

import com.demo.tx.common.DemoResult;
import com.demo.tx.common.DemoRunner;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

import static com.demo.tx.common.DemoRunner.ACCOUNT_ID;

@RestController
@RequestMapping("/demo/basics")
public class BasicsController {

    private static final BigDecimal AMOUNT = new BigDecimal("100.00");

    private final DemoRunner runner;
    private final BasicsService basics;
    private final AccountTxOps txOps;

    public BasicsController(DemoRunner runner, BasicsService basics, AccountTxOps txOps) {
        this.runner = runner;
        this.basics = basics;
        this.txOps = txOps;
    }

    @PostMapping("/self-invocation")
    public DemoResult selfInvocation() {
        return runner.run("Self-invocation (this.method())",
                "Balance dropped even though checkout failed: @Transactional was ignored.",
                () -> basics.checkoutSelfInvocation(ACCOUNT_ID, AMOUNT));
    }

    @PostMapping("/self-invocation-fixed")
    public DemoResult selfInvocationFixed() {
        return runner.run("Same call through another bean",
                "Proxy applied, exception caused a rollback, balance unchanged.",
                () -> basics.checkoutViaProxy(ACCOUNT_ID, AMOUNT));
    }

    @PostMapping("/checked-exception")
    public DemoResult checkedException() {
        return runner.run("Checked exception with default rules",
                "Checked exceptions COMMIT by default. Customer charged for a declined payment.",
                () -> txOps.debitThenDecline(ACCOUNT_ID, AMOUNT));
    }

    @PostMapping("/checked-exception-fixed")
    public DemoResult checkedExceptionFixed() {
        return runner.run("Checked exception with rollbackFor = Exception.class",
                "rollbackFor makes checked exceptions roll back. Balance unchanged.",
                () -> txOps.debitThenDeclineWithRollbackFor(ACCOUNT_ID, AMOUNT));
    }

    @PostMapping("/unexpected-rollback")
    public DemoResult unexpectedRollback() {
        return runner.run("Outer method catches an inner REQUIRED failure",
                "Inner failure marked the shared tx rollback-only. Catching it did not help: nothing was saved.",
                () -> basics.checkoutWithOptionalBonus(ACCOUNT_ID, AMOUNT));
    }
}
