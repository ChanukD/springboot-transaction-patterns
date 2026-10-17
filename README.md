# Spring Boot Transaction Patterns — Runnable Demo

Each pattern has an endpoint that shows the **problem** and the **fix** side by side.
No Docker, no database to install: it uses in-memory H2.

- Spring Boot 4.1.1, Java 21, Spring Data JPA, H2
- 21 tests prove every behaviour: `./mvnw test`

## Run

```bash
./mvnw spring-boot:run
NO_PAUSE=1 ./demo.sh          # or ./demo.sh to step through with pauses
```

Watch the app log. `JpaTransactionManager` is set to DEBUG, so you see every
`Creating new transaction`, `Participating in existing transaction`,
`Suspending current transaction`, and `Rolling back`.

Reset data any time: `curl -X POST localhost:8080/demo/reset` (account #1, balance 1000.00).
H2 console: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:txdemo`).

## Demos

| # | Pattern | Endpoints | Package |
|---|---------|-----------|---------|
| 1 | Proxy traps (self-invocation, checked exceptions, rollback-only) | `/demo/basics/*` | `basics` |
| 2 | `REQUIRES_NEW` audit trail | `/demo/propagation/withdraw?amount=` | `propagation` |
| 3 | `readOnly = true` | `/demo/readonly/*` | `readonly` |
| 4 | `TransactionTemplate` around a slow remote call | `/demo/payments/inside-tx`, `/split-tx` | `template` |
| 5 | Optimistic (+ retry) vs pessimistic locking | `/demo/locking/*` | `locking` |
| 6 | `@TransactionalEventListener(AFTER_COMMIT)` | `/demo/events/order?fail=` | `events` |
| 7 | Transactional outbox + relay with `SKIP LOCKED` | `/demo/outbox/*` | `outbox` |
| 8 | Idempotent consumer | `/demo/idempotency/naive`, `/safe` | `idempotency` |

## Spring Boot 3.x

Everything works the same except retry. Boot 4 / Spring Framework 7 has `@Retryable`
built in (`org.springframework.resilience.annotation`, enable with `@EnableResilientMethods`).
On Boot 3, add `spring-retry` + `spring-boot-starter-aop`, use `@EnableRetry`, and:

```java
@Retryable(retryFor = ObjectOptimisticLockingFailureException.class,
           maxAttempts = 10, backoff = @Backoff(delay = 20, multiplier = 2, maxDelay = 500))
```

On Boot 3 also use `spring-boot-starter-web` instead of `-webmvc`, and inject
Jackson 2's `ObjectMapper` instead of `tools.jackson.databind.json.JsonMapper`.

## Moving to PostgreSQL

Change the datasource URL and add the driver. Everything else stays the same. `SKIP LOCKED`
and the lock-timeout hint behave natively on PostgreSQL, MySQL 8 and Oracle.
