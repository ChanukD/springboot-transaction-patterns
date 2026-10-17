package com.demo.tx.propagation;

import com.demo.tx.common.DemoResult;
import com.demo.tx.common.DemoRunner;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

import static com.demo.tx.common.DemoRunner.ACCOUNT_ID;

@RestController
@RequestMapping("/demo/propagation")
public class PropagationController {

    private final DemoRunner runner;
    private final WithdrawalService withdrawals;
    private final AuditLogRepository audits;

    public PropagationController(DemoRunner runner, WithdrawalService withdrawals, AuditLogRepository audits) {
        this.runner = runner;
        this.withdrawals = withdrawals;
        this.audits = audits;
    }

    /** Try amount=100 (succeeds, 2 audit rows) and amount=999999 (fails, only REQUIRES_NEW row stays). */
    @PostMapping("/withdraw")
    public DemoResult withdraw(@RequestParam BigDecimal amount) {
        return runner.run("Withdraw " + amount + " with audit trail",
                "Check GET /demo/propagation/audit: the REQUIRES_NEW row is always there, the REQUIRED row only on success.",
                () -> withdrawals.withdraw(ACCOUNT_ID, amount));
    }

    @GetMapping("/audit")
    public List<AuditLog> audit() {
        return audits.findAll();
    }
}
