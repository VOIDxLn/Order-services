package com.service.order.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import lombok.Data;
import java.util.UUID;

@Entity
@Table(name = "order_items")
@Data
public class OrderItem {

    @Id
    @GeneratedValue
    private Long id;

    private UUID orderId;
    private UUID eventId;
    private UUID ticketTypeId;

    private int quantity;
    private Double priceSnapshot;
}
