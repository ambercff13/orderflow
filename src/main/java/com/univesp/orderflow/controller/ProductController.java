package com.univesp.orderflow.controller;

import com.univesp.orderflow.dto.ProductDTO;
import com.univesp.orderflow.dto.filters.ProductFilterDTO;
import com.univesp.orderflow.service.ProductService;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
            @PageableDefault(size = 30, sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return productService.getAll(filter, pageable);
    }

    @PatchMapping("/{id}")
    public ProductDTO updateProduct(@PathVariable UUID id, @RequestBody ProductDTO data) {
        return productService.updateProduct(id, data);
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProductDTO uploadImage(@PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        return productService.uploadImage(id, file);
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<Resource> getImage(@PathVariable UUID id) {
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(productService.getImage(id));
    }
}
