package com.univesp.orderflow.controller;

import com.univesp.orderflow.dto.CreateOrderDTO;
import com.univesp.orderflow.dto.OrderDTO;
import com.univesp.orderflow.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public OrderDTO createOrder(@Valid @RequestBody CreateOrderDTO data, Authentication authentication) {
        return orderService.createOrder(data, authentication);
    }

    @GetMapping
    public List<OrderDTO> listOrders(Authentication authentication) {
        return orderService.listOrders(authentication);
    }

    @PatchMapping
    public OrderDTO updateOrderStatus(@RequestBody OrderDTO orderDTO, Authentication authentication) {
        return orderService.updateOrderStatus(orderDTO, authentication);
    }
}
