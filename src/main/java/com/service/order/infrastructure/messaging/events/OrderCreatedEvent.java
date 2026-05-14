package com.service.order.infrastructure.messaging.events;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class OrderCreatedEvent {

    private UUID orderId;
    private UUID eventId;
    private UUID ticketTypeId;
    private int quantity;
}