package com.demo.tx.propagation;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditLogRepository audits;

    public AuditService(AuditLogRepository audits) {
        this.audits = audits;
    }

    /** Joins the caller's transaction. If the caller rolls back, this row disappears too. */
    @Transactional
    public void recordInCallerTx(String message) {
        audits.save(new AuditLog("REQUIRED", message));
    }

    /** Suspends the caller's transaction and commits on its own. Survives the caller's rollback. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordIndependently(String message) {
        audits.save(new AuditLog("REQUIRES_NEW", message));
    }
}
