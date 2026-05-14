package com.service.order.infrastructure.messaging.consumer;

import com.service.order.infrastructure.messaging.events.PaymentResultEvent;
import com.service.order.service.OrderService;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentResultListener {

    private final OrderService orderService;

    @RabbitListener(
        queues = "payment.result.queue",
        queuesToDeclare = @Queue(name = "payment.result.queue", durable = "true")
    )
    public void handlePaymentResult(PaymentResultEvent event) {
        orderService.handlePaymentConfirmationFromEvent(event);
    }
}