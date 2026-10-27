package com.univesp.orderflow.mapper;

import com.univesp.orderflow.dto.ProductDTO;
import com.univesp.orderflow.model.Product;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    Product toEntity(ProductDTO productDTO);
    ProductDTO toDTO(Product product);
    List<ProductDTO> toDTOList(List<Product> products);
    @AfterMapping
    default void fillImageUrl(Product product, @MappingTarget ProductDTO dto) {
        dto.setImageUrl(product.getImageFileName() != null
                ? "/api/products/" + product.getId() + "/image"
                : null);
    }
}
