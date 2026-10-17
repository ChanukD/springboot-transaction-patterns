package com.demo.tx.events;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/demo/events")
public class EventsController {

    private final OrderService orderService;
    private final CustomerOrderRepository orders;
    private final NotificationLog notifications;

    public EventsController(OrderService orderService, CustomerOrderRepository orders, NotificationLog notifications) {
        this.orderService = orderService;
        this.orders = orders;
        this.notifications = notifications;
    }

    @PostMapping("/order")
    public Map<String, Object> placeOrder(@RequestParam(defaultValue = "laptop") String item,
                                          @RequestParam(defaultValue = "false") boolean fail) {
        notifications.clear();
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            result.put("order", orderService.placeOrder(item, fail));
        } catch (RuntimeException e) {
            result.put("error", e.getMessage());
        }
        result.put("ordersInDatabase", orders.count());
        result.put("notificationsSent", notifications.all());
        return result;
    }

    @GetMapping("/notifications")
    public List<String> notifications() {
        return notifications.all();
    }
}
