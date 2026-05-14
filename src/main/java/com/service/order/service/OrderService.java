package com.service.order.service;

import com.service.order.domain.Order;
import com.service.order.domain.OrderStatus;
import com.service.order.infrastructure.redis.TicketReservationService;
import com.service.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

import com.service.order.infrastructure.clients.EventClient;
import com.service.order.infrastructure.clients.dto.EventDTO;
import com.service.order.infrastructure.messaging.events.OrderCreatedEvent;
import com.service.order.infrastructure.messaging.events.PaymentResultEvent;
import com.service.order.controller.dto.CreateOrderRequest;
import com.service.order.controller.dto.PaymentCallback;
import lombok.extern.slf4j.Slf4j;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import io.micrometer.core.instrument.Counter;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final TicketReservationService reservationService;
    private final EventClient eventClient;
    private final RabbitTemplate rabbitTemplate;
    private final MeterRegistry meterRegistry;

    private Counter orderCreatedCounter;
    private Counter orderFailedCounter;

    @PostConstruct
    public void initMetrics() {
        this.orderCreatedCounter = meterRegistry.counter("orders.created");
        this.orderFailedCounter = meterRegistry.counter("orders.failed");
    }

    public Order createOrder(CreateOrderRequest request) {

        log.info("Iniciando creación de orden para evento {} con cantidad {}",
                request.getEventId(), request.getQuantity());

        // 1. Consultar evento
        // EventDTO event; --> trabaja con microservicio de eventos.
        // Codigo para validar sin depender del microservicio de eventos:
        EventDTO event = new EventDTO();
        event.setAvailableTickets(100);

        /* Cuando se trabaje con el microservicio de eventos se debe de descomentar el bloque:
         * try {
         * event = eventClient.getEvent(request.getEventId());
         * } catch (Exception e) {
         * log.error("Error consultando Event Service para evento {}",
         * request.getEventId(), e);
         * throw new RuntimeException("Event Service no disponible");
         * }
         * 
         * if (event == null) {
         * log.warn("Evento no encontrado: {}", request.getEventId());
         * throw new RuntimeException("Evento no encontrado");
         * }
         */

        if (request.getQuantity() <= 0) {
            log.warn("Cantidad inválida: {}", request.getQuantity());
            throw new RuntimeException("Cantidad inválida");
        }

        // VALIDACIÓN
        if (event.getAvailableTickets() < request.getQuantity()) {
            log.warn("No hay suficientes cupos en Event Service para evento {}", request.getEventId());
            throw new RuntimeException("No hay suficientes cupos disponibles");
        }

        String redisKey = "tickets:event:" + request.getEventId() +
                ":type:" + request.getTicketTypeId();

        // 2. Reservar en Redis
        log.info("Reservando {} tickets en Redis para evento {}",
                request.getQuantity(), request.getEventId());

        boolean reserved = reservationService.reserveTickets(redisKey, request.getQuantity());

        if (!reserved) {
            log.warn("No se pudieron reservar tickets en Redis para evento {}", request.getEventId());
            orderFailedCounter.increment();
            throw new RuntimeException("No hay cupos disponibles");
        }

        // 3. Crear orden
        Order order = new Order();
        order.setEventId(request.getEventId());
        order.setTicketTypeId(request.getTicketTypeId());
        order.setQuantity(request.getQuantity());
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order.setExpiresAt(LocalDateTime.now().plusMinutes(5));

        orderRepository.save(order);

        orderCreatedCounter.increment();

        log.info("Orden creada con ID {}", order.getId());

        // 4. Crear evento
        OrderCreatedEvent eventMessage = new OrderCreatedEvent(
                order.getId(),
                order.getEventId(),
                order.getTicketTypeId(),
                order.getQuantity());

        // 5. Enviar a RabbitMQ con rollback
        try {
            rabbitTemplate.convertAndSend(
                    "order.exchange",
                    "order.created",
                    eventMessage);

            log.info("Evento enviado a RabbitMQ para orden {}", order.getId());

        } catch (Exception e) {

            log.error("Error enviando evento a RabbitMQ para orden {}", order.getId(), e);

            // rollback lógico
            order.setStatus(OrderStatus.FAILED);
            orderRepository.save(order);

            // liberar cupos
            reservationService.releaseTickets(redisKey, request.getQuantity());

            orderFailedCounter.increment();

            log.warn("Rollback aplicado: orden {} marcada como FAILED y cupos liberados", order.getId());

            throw new RuntimeException("Error enviando evento a RabbitMQ");
        }

        return order;
    }

    public void handlePaymentConfirmation(PaymentCallback callback) {

        log.info("Procesando callback de pago para orden {}", callback.getOrderId());

        Order order = orderRepository.findById(callback.getOrderId())
                .orElseThrow();

        String redisKey = "tickets:event:" +
                order.getEventId() + ":type:" + order.getTicketTypeId();

        // evita doble procesamiento
        if (order.getStatus() != OrderStatus.PENDING) {
            log.warn("Orden {} ya procesada, ignorando callback", order.getId());
            return;
        }

        if (callback.isSuccess()) {
            order.setStatus(OrderStatus.PAID);
            log.info("Orden {} marcada como PAID", order.getId());
        } else {
            order.setStatus(OrderStatus.FAILED);

            reservationService.releaseTickets(redisKey, order.getQuantity());

            log.warn("Pago fallido para orden {}, cupos liberados", order.getId());
        }

        orderRepository.save(order);
    }

    public void handlePaymentConfirmationFromEvent(PaymentResultEvent event) {

        log.info("Procesando confirmación de pago (evento) para orden {}", event.getOrderId());

        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow();

        String redisKey = "tickets:event:" +
                order.getEventId() + ":type:" + order.getTicketTypeId();

        // idempotencia
        if (order.getStatus() != OrderStatus.PENDING) {
            log.warn("Orden {} ya procesada, ignorando evento", order.getId());
            return;
        }

        if (event.isSuccess()) {
            order.setStatus(OrderStatus.PAID);
            log.info("Orden {} marcada como PAID", order.getId());
        } else {
            order.setStatus(OrderStatus.FAILED);

            reservationService.releaseTickets(redisKey, order.getQuantity());

            log.warn("Pago fallido para orden {}, cupos liberados", order.getId());
        }

        orderRepository.save(order);
    }
}