package com.univesp.orderflow.service.impl;

import com.univesp.orderflow.dto.ProductDTO;
import com.univesp.orderflow.dto.filters.ProductFilterDTO;
import com.univesp.orderflow.exceptions.ApiErrorException;
import com.univesp.orderflow.mapper.ProductMapper;
import com.univesp.orderflow.model.Product;
import com.univesp.orderflow.repository.ProductRepository;
import com.univesp.orderflow.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

@Service
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public ProductServiceImpl(ProductRepository productRepository, ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
    }

    @Override
    public ProductDTO saveProduct(ProductDTO data) {
        return this.productMapper.toDTO(this.productRepository.save(this.productMapper.toEntity(data)));
    }

    @Override
    public Page<ProductDTO> getAll(ProductFilterDTO filterDTO, Pageable pageable) {
        return this.productRepository.findByFilters(filterDTO.getName(), filterDTO.getCode(), pageable)
                .map(this.productMapper::toDTO);
    }

    @Override
    public ProductDTO updateProduct(UUID id, ProductDTO data) {
        Product product = this.productRepository.findById(id)
                .orElseThrow(() -> new ApiErrorException(HttpStatus.NOT_FOUND, "Produto não encontrado."));

        if(Objects.nonNull(data.getName())){
            product.setName(data.getName());
        }

        if(Objects.nonNull(data.getCode())){
            product.setCode(data.getCode());
        }

        if(Objects.nonNull(data.getPrice())){
            product.setPrice(data.getPrice());
        }

        if(Objects.nonNull(data.getQuantity())){
            product.setQuantity(data.getQuantity());
        }

        return this.productMapper.toDTO(this.productRepository.save(product));
    }
}
