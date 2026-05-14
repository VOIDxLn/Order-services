package com.service.order.controller.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class PaymentCallback {
    private UUID orderId;
    private boolean success;
    private String redisKey;
    private int quantity;
}