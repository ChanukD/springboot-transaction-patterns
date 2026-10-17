package com.demo.tx.outbox;

import com.demo.tx.events.CustomerOrder;
import com.demo.tx.events.CustomerOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

@Service
public class OutboxOrderService {

    private final CustomerOrderRepository orders;
    private final OutboxEventRepository outbox;
    private final JsonMapper json;

    public OutboxOrderService(CustomerOrderRepository orders, OutboxEventRepository outbox, JsonMapper json) {
        this.orders = orders;
        this.outbox = outbox;
        this.json = json;
    }

    /** Order row + event row in ONE local transaction. Both commit, or neither does. No broker call here. */
    @Transactional
    public CustomerOrder placeOrder(String item, boolean fail) {
        CustomerOrder order = orders.save(new CustomerOrder(item));

        String payload = json.writeValueAsString(Map.of("orderId", order.getId(), "item", item));
        outbox.save(new OutboxEvent("orders.placed", String.valueOf(order.getId()), payload));

        if (fail) {
            throw new IllegalStateException("validation failed after writing order + outbox row");
        }
        return order;
    }
}
