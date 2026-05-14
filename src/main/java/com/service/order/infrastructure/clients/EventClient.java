package com.service.order.infrastructure.clients;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.service.order.infrastructure.clients.dto.EventDTO;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EventClient {

    private final RestTemplate restTemplate;

    public EventDTO getEvent(UUID eventId) {
        return restTemplate.getForObject(
            "http://event-service/events/" + eventId,
            EventDTO.class
        );
    }
}