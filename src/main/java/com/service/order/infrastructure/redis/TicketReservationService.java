package com.service.order.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TicketReservationService {

    private final StringRedisTemplate redisTemplate;

    public boolean reserveTickets(String key, int quantity) {
    try {
        Long remaining = redisTemplate.opsForValue().decrement(key, quantity);

        if (remaining == null || remaining < 0) {
            redisTemplate.opsForValue().increment(key, quantity);
            return false;
        }

        return true;

    } catch (Exception e) {
        e.printStackTrace();
        throw e;
    }
}

    public void releaseTickets(String key, int quantity) {
        redisTemplate.opsForValue().increment(key, quantity);
    }
}