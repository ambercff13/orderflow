package com.univesp.orderflow.repository;

import com.univesp.orderflow.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    @Query("SELECT p FROM Product p WHERE " +
            "(:name IS NULL OR :name = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) OR " +
            "(:code IS NULL OR :code = '' OR LOWER(p.code) LIKE LOWER(CONCAT('%', :code, '%')))")
    Page<Product> findByFilters(
            @Param("name") String name,
            @Param("code") String code,
            Pageable pageable
    );
}
