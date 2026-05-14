package com.service.order.controller;

import com.service.order.controller.dto.CreateOrderRequest;
import com.service.order.controller.dto.PaymentCallback;
import com.service.order.domain.Order;
import com.service.order.service.OrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Gestión de órdenes de compra")
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "Crear una nueva orden")
    @PostMapping
    public ResponseEntity<Order> create(@Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.ok(orderService.createOrder(request));
    }

    @Operation(summary = "Confirmar pago de una orden")
    @PostMapping("/payment-confirmation")
    public ResponseEntity<Void> confirmPayment(@Valid @RequestBody PaymentCallback callback) {

        orderService.handlePaymentConfirmation(callback);

        return ResponseEntity.ok().build();
    }

}
