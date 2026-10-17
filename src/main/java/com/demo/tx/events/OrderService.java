package com.demo.tx.events;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final CustomerOrderRepository orders;
    private final ApplicationEventPublisher events;

    public OrderService(CustomerOrderRepository orders, ApplicationEventPublisher events) {
        this.orders = orders;
        this.events = events;
    }

    @Transactional
    public CustomerOrder placeOrder(String item, boolean failAfterPublish) {
        CustomerOrder order = orders.save(new CustomerOrder(item));
        events.publishEvent(new OrderPlaced(order.getId(), item));

        if (failAfterPublish) {   // e.g. a constraint violation or a stock check failing later in the method
            throw new IllegalStateException("stock check failed after the event was published");
        }
        return order;
    }
}
