#!/usr/bin/env bash
# Walks through every transaction demo. Start the app first: ./mvnw spring-boot:run
# Optional: install jq for pretty output.
set -euo pipefail

BASE=${BASE:-http://localhost:8080}
PRETTY=cat; command -v jq >/dev/null && PRETTY="jq ."

call() { echo; echo ">>> $1 $2"; curl -s -X "$1" "$BASE$2" | $PRETTY; }
step() { echo; echo "=================================================="; echo "  $1"; echo "=================================================="; }
pause() { [[ "${NO_PAUSE:-}" == "1" ]] || read -rp "  [enter] to continue..." _; }
reset() { curl -s -X POST "$BASE/demo/reset" >/dev/null; }

step "1. Proxy traps"
reset
call POST /demo/basics/self-invocation          # balance drops, tx ignored
call POST /demo/basics/self-invocation-fixed    # rolled back
call POST /demo/basics/checked-exception        # commits on checked exception
call POST /demo/basics/checked-exception-fixed  # rollbackFor
call POST /demo/basics/unexpected-rollback      # UnexpectedRollbackException
pause

step "2. REQUIRES_NEW audit trail"
reset
call POST "/demo/propagation/withdraw?amount=100"
call POST "/demo/propagation/withdraw?amount=999999"
call GET  /demo/propagation/audit
pause

step "3. Read-only transactions"
reset
call POST /demo/readonly/modify-in-readonly
call POST "/demo/readonly/rename?owner=bob"
pause

step "4. Slow remote call: inside vs outside the transaction"
reset
call POST "/demo/payments/inside-tx?amount=60&decline=true"
call POST "/demo/payments/split-tx?amount=80&decline=true"
call GET  /demo/payments
pause

step "5. Concurrent updates: 8 debits at once"
reset; call POST /demo/locking/optimistic-no-retry
reset; call POST /demo/locking/optimistic-retry
reset; call POST /demo/locking/pessimistic
pause

step "6. Events: @EventListener vs AFTER_COMMIT"
reset
call POST "/demo/events/order?item=laptop"
call POST "/demo/events/order?item=camera&fail=true"
pause

step "7. Outbox during a broker outage"
reset
call POST "/demo/outbox/order?item=phone&fail=true"
call POST "/demo/outbox/broker?down=true"
call POST "/demo/outbox/order?item=phone"
call POST /demo/outbox/relay
call POST "/demo/outbox/broker?down=false"
echo "  waiting for the scheduled relay..."; sleep 4
call GET  /demo/outbox
pause

step "8. Duplicate message delivery"
reset; call POST "/demo/idempotency/naive?eventId=pay-001&deliveries=3"
reset; call POST "/demo/idempotency/safe?eventId=pay-002&deliveries=3"
call POST "/demo/idempotency/safe?eventId=pay-003&deliveries=5&concurrent=true"

echo; echo "Done. Watch the app log for: Creating new / Participating / Suspending / Rolling back."
