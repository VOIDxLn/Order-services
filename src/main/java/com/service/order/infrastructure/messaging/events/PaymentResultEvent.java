package com.service.order.infrastructure.messaging.events;

import lombok.Data;

import java.util.UUID;

@Data
public class PaymentResultEvent {

    private UUID orderId;
    private boolean success;
}