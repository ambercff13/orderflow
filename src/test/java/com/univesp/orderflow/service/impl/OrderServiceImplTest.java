package com.univesp.orderflow.service.impl;

import com.univesp.orderflow.dto.CreateOrderDTO;
import com.univesp.orderflow.dto.CreateOrderItemDTO;
import com.univesp.orderflow.dto.OrderDTO;
import com.univesp.orderflow.exceptions.ApiErrorException;
import com.univesp.orderflow.mapper.OrderMapper;
import com.univesp.orderflow.model.Order;
import com.univesp.orderflow.model.Product;
import com.univesp.orderflow.model.User;
import com.univesp.orderflow.model.enums.OrderStatus;
import com.univesp.orderflow.repository.OrderRepository;
import com.univesp.orderflow.repository.ProductRepository;
import com.univesp.orderflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private OrderMapper orderMapper;

    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(orderRepository, productRepository, userRepository, orderMapper);
    }

    @Test
    void createOrderDecreasesStockAndPersistsItemAtCurrentPrice() {
        User user = user("client@example.com");
        Product product = product("Coffee", 5, new BigDecimal("3.75"));
        UUID productId = UUID.randomUUID();
        OrderDTO expected = new OrderDTO();
        var authentication = authentication("client@example.com", false);
        when(userRepository.findByEmail("client@example.com")).thenReturn(Optional.of(user));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderMapper.toDTO(any(Order.class))).thenReturn(expected);

        OrderDTO result = orderService.createOrder(
                new CreateOrderDTO(List.of(new CreateOrderItemDTO(productId, 2))),
                authentication
        );

        assertSame(expected, result);
        assertEquals(3, product.getQuantity());
        verify(orderRepository).save(argThat(order ->
                order.getUser() == user
                        && order.getItems().size() == 1
                        && order.getItems().get(0).getQuantity() == 2
                        && order.getItems().get(0).getPrice().equals(new BigDecimal("3.75"))
                        && order.getItems().get(0).getOrder() == order
        ));
    }

    @Test
    void createOrderRejectsInsufficientStock() {
        UUID productId = UUID.randomUUID();
        User user = user("client@example.com");
        Product product = product("Coffee", 1, new BigDecimal("3.75"));
        when(userRepository.findByEmail("client@example.com")).thenReturn(Optional.of(user));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        ApiErrorException exception = assertThrows(
                ApiErrorException.class,
                () -> orderService.createOrder(
                        new CreateOrderDTO(List.of(new CreateOrderItemDTO(productId, 2))),
                        authentication("client@example.com", false)
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals(1, product.getQuantity());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrderReturnsNotFoundForUnknownProduct() {
        UUID productId = UUID.randomUUID();
        when(userRepository.findByEmail("client@example.com"))
                .thenReturn(Optional.of(user("client@example.com")));
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        ApiErrorException exception = assertThrows(
                ApiErrorException.class,
                () -> orderService.createOrder(
                        new CreateOrderDTO(List.of(new CreateOrderItemDTO(productId, 1))),
                        authentication("client@example.com", false)
                )
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void listOrdersReturnsAllOrdersForManager() {
        Order order = new Order();
        OrderDTO dto = new OrderDTO();
        when(orderRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(order));
        when(orderMapper.toDTO(order)).thenReturn(dto);

        List<OrderDTO> result = orderService.listOrders(authentication("manager@example.com", true));

        assertEquals(List.of(dto), result);
        verify(orderRepository).findAllByOrderByCreatedAtDesc();
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void listOrdersReturnsOnlyCurrentClientOrders() {
        User user = user("client@example.com");
        user.setId(UUID.randomUUID());
        Order order = new Order();
        OrderDTO dto = new OrderDTO();
        when(userRepository.findByEmail("client@example.com")).thenReturn(Optional.of(user));
        when(orderRepository.findAllByUser_IdOrderByCreatedAtDesc(user.getId())).thenReturn(List.of(order));
        when(orderMapper.toDTO(order)).thenReturn(dto);

        List<OrderDTO> result = orderService.listOrders(authentication("client@example.com", false));

        assertEquals(List.of(dto), result);
        verify(orderRepository).findAllByUser_IdOrderByCreatedAtDesc(user.getId());
    }

    @Test
    void updateOrderStatusPersistsNewStatus() {
        UUID id = UUID.randomUUID();
        Order order = new Order();
        OrderDTO changes = new OrderDTO();
        changes.setId(id);
        changes.setOrderStatus(OrderStatus.COMPLETED);
        OrderDTO expected = new OrderDTO();
        when(orderRepository.findById(id)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.toDTO(order)).thenReturn(expected);

        assertSame(expected, orderService.updateOrderStatus(changes, authentication("manager@example.com", true)));
        assertEquals(OrderStatus.COMPLETED, order.getStatus());
    }

    @Test
    void updateOrderStatusReturnsNotFoundForUnknownOrder() {
        UUID id = UUID.randomUUID();
        OrderDTO changes = new OrderDTO();
        changes.setId(id);
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        ApiErrorException exception = assertThrows(
                ApiErrorException.class,
                () -> orderService.updateOrderStatus(changes, authentication("manager@example.com", true))
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(orderRepository, never()).save(any());
    }

    private User user(String email) {
        User user = new User();
        user.setEmail(email);
        user.setName("Client");
        return user;
    }

    private Product product(String name, int quantity, BigDecimal price) {
        Product product = new Product();
        product.setName(name);
        product.setQuantity(quantity);
        product.setPrice(price);
        return product;
    }

    private UsernamePasswordAuthenticationToken authentication(String email, boolean manager) {
        var authorities = manager
                ? List.of(new SimpleGrantedAuthority("ROLE_MANAGER"))
                : List.of(new SimpleGrantedAuthority("ROLE_CLIENT"));
        return UsernamePasswordAuthenticationToken.authenticated(email, null, authorities);
    }
}
