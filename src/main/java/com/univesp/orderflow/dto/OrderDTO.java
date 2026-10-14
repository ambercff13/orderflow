package com.univesp.orderflow.dto;

import com.univesp.orderflow.model.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrderDTO {
    private UUID id;
    private Long number;
    private String userName;
    private List<OrderItemDTO> orderItems;
    private OrderStatus orderStatus;
    private LocalDateTime createdAt;
}
