package com.demo.tx.template;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/demo/payments")
public class PaymentController {

    private final PaymentService service;
    private final PaymentRepository payments;

    public PaymentController(PaymentService service, PaymentRepository payments) {
        this.service = service;
        this.payments = payments;
    }

    @PostMapping("/inside-tx")
    public Object payInsideTx(@RequestParam(defaultValue = "50") BigDecimal amount,
                              @RequestParam(defaultValue = "false") boolean decline) {
        try {
            return service.payInsideTransaction(amount, decline);
        } catch (RuntimeException e) {
            return Map.of("error", e.getMessage(),
                    "lesson", "The PENDING row was rolled back. No record that we tried to charge the customer.");
        }
    }

    @PostMapping("/split-tx")
    public PaymentService.PayResult paySplitTx(@RequestParam(defaultValue = "50") BigDecimal amount,
                                               @RequestParam(defaultValue = "false") boolean decline) {
        return service.pay(amount, decline);
    }

    @GetMapping
    public List<Payment> all() {
        return payments.findAll();
    }
}
