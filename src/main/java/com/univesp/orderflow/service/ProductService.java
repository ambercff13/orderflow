package com.univesp.orderflow.service;

import com.univesp.orderflow.dto.ProductDTO;
import com.univesp.orderflow.dto.filters.ProductFilterDTO;
import com.univesp.orderflow.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ProductService {
    ProductDTO saveProduct(ProductDTO data);
    Page<ProductDTO> getAll(ProductFilterDTO filterDTO, Pageable pageable);
    ProductDTO updateProduct(UUID id, ProductDTO data);
}
