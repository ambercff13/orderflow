package com.univesp.orderflow.service;

import com.univesp.orderflow.dto.ProductDTO;
import com.univesp.orderflow.dto.filters.ProductFilterDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

import java.util.List;
import java.util.UUID;

public interface ProductService {
    ProductDTO saveProduct(ProductDTO data);
    Page<ProductDTO> getAll(ProductFilterDTO filterDTO, Pageable pageable);
    ProductDTO updateProduct(UUID id, ProductDTO data);
    ProductDTO uploadImage(UUID id, MultipartFile file);
    Resource getImage(UUID id);
}
