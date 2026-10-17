package com.demo.tx.idempotency;

import com.demo.tx.common.DemoRunner;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.demo.tx.common.DemoRunner.ACCOUNT_ID;

/** Delivers the SAME message several times at once, like a broker redelivering after a timeout. */
@RestController
@RequestMapping("/demo/idempotency")
public class IdempotencyController {

    private final DemoRunner runner;
    private final PaymentReceivedHandler handler;
    private final PaymentReceivedConsumer consumer;

    public IdempotencyController(DemoRunner runner, PaymentReceivedHandler handler, PaymentReceivedConsumer consumer) {
        this.runner = runner;
        this.handler = handler;
        this.consumer = consumer;
    }

    /** concurrent=false: redelivered one after another (typical after a consumer timeout). */
    @PostMapping("/naive")
    public Map<String, Object> naive(@RequestParam String eventId,
                                     @RequestParam(defaultValue = "100") BigDecimal amount,
                                     @RequestParam(defaultValue = "3") int deliveries,
                                     @RequestParam(defaultValue = "false") boolean concurrent) throws Exception {
        PaymentReceived event = new PaymentReceived(eventId, ACCOUNT_ID, amount);
        return deliver(event, deliveries, concurrent, () -> {
            handler.applyNaive(event);
            return "APPLIED";
        });
    }

    @PostMapping("/safe")
    public Map<String, Object> safe(@RequestParam String eventId,
                                    @RequestParam(defaultValue = "100") BigDecimal amount,
                                    @RequestParam(defaultValue = "3") int deliveries,
                                    @RequestParam(defaultValue = "false") boolean concurrent) throws Exception {
        PaymentReceived event = new PaymentReceived(eventId, ACCOUNT_ID, amount);
        return deliver(event, deliveries, concurrent, () -> consumer.onMessage(event).name());
    }

    private Map<String, Object> deliver(PaymentReceived event, int deliveries, boolean concurrent,
                                        Callable<String> handle) throws Exception {
        BigDecimal before = runner.balance();
        List<String> outcomes = new ArrayList<>();

        if (!concurrent) {
            for (int i = 0; i < deliveries; i++) {
                outcomes.add(safeCall(handle));
            }
        } else {
            CountDownLatch start = new CountDownLatch(1);
            try (ExecutorService pool = Executors.newFixedThreadPool(deliveries)) {
                List<Future<String>> futures = new ArrayList<>();
                for (int i = 0; i < deliveries; i++) {
                    futures.add(pool.submit(() -> {
                        start.await();
                        return safeCall(handle);
                    }));
                }
                start.countDown();
                for (Future<String> f : futures) {
                    outcomes.add(f.get());
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("eventId", event.eventId());
        result.put("deliveries", deliveries);
        result.put("outcomes", outcomes);
        result.put("balanceBefore", before);
        result.put("balanceAfter", runner.balance());
        result.put("expectedAfter", before.add(event.amount()));
        return result;
    }

    private static String safeCall(Callable<String> handle) {
        try {
            return handle.call();
        } catch (Exception e) {
            return "ERROR " + e.getClass().getSimpleName();
        }
    }
}
