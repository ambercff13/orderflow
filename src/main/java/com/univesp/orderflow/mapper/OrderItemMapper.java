package com.univesp.orderflow.mapper;

import com.univesp.orderflow.dto.OrderItemDTO;
import com.univesp.orderflow.model.OrderItem;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = ProductMapper.class)
public interface OrderItemMapper {
    OrderItemDTO toDTO(OrderItem orderItem);
}
