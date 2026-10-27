package com.univesp.orderflow.service;

import com.univesp.orderflow.dto.CreateOrderDTO;
import com.univesp.orderflow.dto.OrderDTO;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.UUID;

public interface OrderService {
    OrderDTO createOrder(CreateOrderDTO orderDTO, Authentication authentication);
    List<OrderDTO> listOrders(Authentication authentication);
    OrderDTO updateOrderStatus(OrderDTO orderDTO, Authentication authentication);
}
