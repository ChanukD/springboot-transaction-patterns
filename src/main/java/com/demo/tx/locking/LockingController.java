package com.demo.tx.locking;

import com.demo.tx.common.DemoRunner;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static com.demo.tx.common.DemoRunner.ACCOUNT_ID;

/** Fires N debits at the same account at the same moment. */
@RestController
@RequestMapping("/demo/locking")
public class LockingController {

    public record ConcurrencyResult(String strategy, int requests, int succeeded, int failed,
                                    Integer totalAttempts, BigDecimal balanceBefore, BigDecimal balanceAfter,
                                    BigDecimal expectedBalanceIfAllSucceeded, List<String> errors) {
    }

    private static final BigDecimal AMOUNT = new BigDecimal("10.00");

    private final DemoRunner runner;
    private final LockingTxService txService;
    private final RetryingAccountService retrying;

    public LockingController(DemoRunner runner, LockingTxService txService, RetryingAccountService retrying) {
        this.runner = runner;
        this.txService = txService;
        this.retrying = retrying;
    }

    @PostMapping("/optimistic-no-retry")
    public ConcurrencyResult optimisticNoRetry(@RequestParam(defaultValue = "8") int threads) throws Exception {
        txService.resetAttempts();
        return runConcurrently("optimistic, no retry", threads,
                () -> txService.debitOptimistic(ACCOUNT_ID, AMOUNT), true);
    }

    @PostMapping("/optimistic-retry")
    public ConcurrencyResult optimisticRetry(@RequestParam(defaultValue = "8") int threads) throws Exception {
        txService.resetAttempts();
        return runConcurrently("optimistic + @Retryable", threads,
                () -> retrying.debit(ACCOUNT_ID, AMOUNT), true);
    }

    @PostMapping("/pessimistic")
    public ConcurrencyResult pessimistic(@RequestParam(defaultValue = "8") int threads) throws Exception {
        return runConcurrently("pessimistic (SELECT FOR UPDATE)", threads,
                () -> txService.debitPessimistic(ACCOUNT_ID, AMOUNT), false);
    }

    private ConcurrencyResult runConcurrently(String strategy, int threads, Runnable debit,
                                              boolean countAttempts) throws Exception {
        BigDecimal before = runner.balance();
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger ok = new AtomicInteger();
        List<String> errors = new ArrayList<>();
        ConcurrentHashMap<String, Integer> errorCounts = new ConcurrentHashMap<>();

        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    try {
                        debit.run();
                        ok.incrementAndGet();
                    } catch (RuntimeException e) {
                        errorCounts.merge(e.getClass().getSimpleName(), 1, Integer::sum);
                    }
                    return null;
                }));
            }
            start.countDown();   // release all threads together
            for (Future<?> f : futures) {
                f.get();
            }
        }
        errorCounts.forEach((type, count) -> errors.add(count + " x " + type));

        BigDecimal expected = before.subtract(AMOUNT.multiply(BigDecimal.valueOf(threads)));
        return new ConcurrencyResult(strategy, threads, ok.get(), threads - ok.get(),
                countAttempts ? txService.attempts() : null,
                before, runner.balance(), expected, errors);
    }
}
