package com.univesp.orderflow.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateOrderDTO(@NotEmpty List<@Valid CreateOrderItemDTO> items) {
}
