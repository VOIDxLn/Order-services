package com.service.order.infrastructure.clients.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class EventDTO {
    // private String id; para trabajar con microservicio de eventos
    private UUID id; // para realizar pruebas sin depender del microservicio de eventos
    private String name;
    private int availableTickets;
}
