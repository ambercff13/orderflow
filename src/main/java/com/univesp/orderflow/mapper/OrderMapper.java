package com.univesp.orderflow.mapper;

import com.univesp.orderflow.dto.OrderDTO;
import com.univesp.orderflow.model.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = OrderItemMapper.class)
public interface OrderMapper {
    @Mapping(source = "user.name", target = "userName")
    @Mapping(source = "items", target = "orderItems")
    @Mapping(source = "status", target = "orderStatus")
    OrderDTO toDTO(Order order);
}
