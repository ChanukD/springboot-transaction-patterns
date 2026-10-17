package com.demo.tx.outbox;

import com.demo.tx.events.CustomerOrderRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/demo/outbox")
public class OutboxController {

    private final OutboxOrderService orderService;
    private final OutboxRelay relay;
    private final OutboxEventRepository outbox;
    private final CustomerOrderRepository orders;
    private final InMemoryBroker broker;

    public OutboxController(OutboxOrderService orderService, OutboxRelay relay, OutboxEventRepository outbox,
                            CustomerOrderRepository orders, InMemoryBroker broker) {
        this.orderService = orderService;
        this.relay = relay;
        this.outbox = outbox;
        this.orders = orders;
        this.broker = broker;
    }

    @PostMapping("/order")
    public Map<String, Object> placeOrder(@RequestParam(defaultValue = "phone") String item,
                                          @RequestParam(defaultValue = "false") boolean fail) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            result.put("order", orderService.placeOrder(item, fail));
        } catch (RuntimeException e) {
            result.put("error", e.getMessage());
        }
        result.putAll(state());
        return result;
    }

    /** Simulate a Kafka outage: POST /demo/outbox/broker?down=true */
    @PostMapping("/broker")
    public Map<String, Object> broker(@RequestParam boolean down) {
        broker.setDown(down);
        return state();
    }

    /** Run the relay now instead of waiting for the scheduler. */
    @PostMapping("/relay")
    public Map<String, Object> relayNow() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("publishedNow", relay.relayBatch());
        result.putAll(state());
        return result;
    }

    @GetMapping
    public Map<String, Object> state() {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("brokerDown", broker.isDown());
        state.put("ordersInDatabase", orders.count());
        state.put("outboxRows", outbox.count());
        state.put("outboxPending", outbox.countByPublishedAtIsNull());
        state.put("deliveredToBroker", broker.delivered());
        return state;
    }
}
