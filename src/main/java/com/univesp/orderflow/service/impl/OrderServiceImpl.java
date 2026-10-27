package com.univesp.orderflow.service.impl;

import com.univesp.orderflow.dto.CreateOrderDTO;
import com.univesp.orderflow.dto.OrderDTO;
import com.univesp.orderflow.exceptions.ApiErrorException;
import com.univesp.orderflow.mapper.OrderMapper;
import com.univesp.orderflow.model.Order;
import com.univesp.orderflow.model.OrderItem;
import com.univesp.orderflow.model.Product;
import com.univesp.orderflow.model.User;
import com.univesp.orderflow.repository.OrderRepository;
import com.univesp.orderflow.repository.ProductRepository;
import com.univesp.orderflow.repository.UserRepository;
import com.univesp.orderflow.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderMapper orderMapper;

    public OrderServiceImpl(
            OrderRepository orderRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            OrderMapper orderMapper
    ) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.orderMapper = orderMapper;
    }

    @Override
    @Transactional
    public OrderDTO createOrder(CreateOrderDTO orderDTO, Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ApiErrorException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
        Order order = new Order();
        order.setUser(user);

        for (var itemData : orderDTO.items()) {
            Product product = productRepository.findById(itemData.productId())
                    .orElseThrow(() -> new ApiErrorException(HttpStatus.NOT_FOUND, "Produto não encontrado."));

            if (itemData.quantity() > product.getQuantity()) {
                throw new ApiErrorException(HttpStatus.BAD_REQUEST,
                        "Estoque insuficiente para o produto " + product.getName() + ".");
            }

            product.setQuantity(product.getQuantity() - itemData.quantity());

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setQuantity(itemData.quantity());
            item.setPrice(product.getPrice());
            order.getItems().add(item);
        }

        return orderMapper.toDTO(orderRepository.save(order));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDTO> listOrders(Authentication authentication) {
        List<Order> orders;
        if (authentication.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_MANAGER"))) {
            orders = orderRepository.findAllByOrderByCreatedAtDesc();
        } else {
            User user = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new ApiErrorException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
            orders = orderRepository.findAllByUser_IdOrderByCreatedAtDesc(user.getId());
        }
        return orders.stream().map(orderMapper::toDTO).toList();
    }

    @Override
    public OrderDTO updateOrderStatus(OrderDTO orderDTO, Authentication authentication) {
        Order order = orderRepository.findById(orderDTO.getId())
                .orElseThrow(() -> new ApiErrorException(HttpStatus.NOT_FOUND, "Pedido não encontrado."));
        order.setStatus(orderDTO.getOrderStatus());
        return orderMapper.toDTO(orderRepository.save(order));
    }
}
