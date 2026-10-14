package com.univesp.orderflow.mapper;

import com.univesp.orderflow.dto.ProductDTO;
import com.univesp.orderflow.model.Product;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    Product toEntity(ProductDTO productDTO);
    ProductDTO toDTO(Product product);
    List<ProductDTO> toDTOList(List<Product> products);
}
