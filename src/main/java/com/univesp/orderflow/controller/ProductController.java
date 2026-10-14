package com.univesp.orderflow.controller;

import com.univesp.orderflow.dto.ProductDTO;
import com.univesp.orderflow.dto.filters.ProductFilterDTO;
import com.univesp.orderflow.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ProductDTO createProduct(@RequestBody ProductDTO data) {
        return productService.saveProduct(data);
    }

    @GetMapping
    public Page<ProductDTO> listProducts(
            @ModelAttribute ProductFilterDTO filter,
            Pageable pageable
    ) {
        return productService.getAll(filter, pageable);
    }

    @PatchMapping("/{id}")
    public ProductDTO updateProduct(@PathVariable UUID id, @RequestBody ProductDTO data) {
        return productService.updateProduct(id, data);
    }
}
