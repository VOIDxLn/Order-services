package com.service.order.controller.dto;

import lombok.Data;
import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
public class CreateOrderRequest {

    @Schema(description = "ID del evento", example = "1")
    @NotNull
    private UUID eventId;

    @Schema(description = "ID del tipo de ticket", example = "10")
    @NotNull
    private UUID ticketTypeId;

    @Schema(description = "Cantidad de tickets", example = "2")
    @Min(1)
    private int quantity;
}
