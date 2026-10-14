package com.univesp.orderflow.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record CreateOrderItemDTO(
        @NotNull UUID productId,
        @NotNull @Positive Integer quantity
) {
}
