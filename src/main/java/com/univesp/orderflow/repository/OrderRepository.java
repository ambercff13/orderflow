package com.univesp.orderflow.repository;

import com.univesp.orderflow.model.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    @EntityGraph(attributePaths = {"user", "items", "items.product"})
    List<Order> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"user", "items", "items.product"})
    List<Order> findAllByUser_IdOrderByCreatedAtDesc(UUID userId);
}
