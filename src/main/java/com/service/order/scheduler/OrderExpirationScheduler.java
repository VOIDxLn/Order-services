package com.service.order.scheduler;

import com.service.order.domain.Order;
import com.service.order.domain.OrderStatus;
import com.service.order.infrastructure.redis.TicketReservationService;
import com.service.order.repository.OrderRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class OrderExpirationScheduler {

    private final OrderRepository orderRepository;
    private final TicketReservationService reservationService;

    @Scheduled(fixedRate = 60000) // cada 60 segundos
    public void expireOrders() {

        var now = LocalDateTime.now();

        var expiredOrders = orderRepository.findExpiredOrders(now);

        for (Order order : expiredOrders) {

            order.setStatus(OrderStatus.EXPIRED);

            // liberar cupos
            String redisKey = "tickets:event:" +
                    order.getEventId() + ":type:" + order.getTicketTypeId();

            reservationService.releaseTickets(redisKey, order.getQuantity());

            orderRepository.save(order);
        }
    }
}