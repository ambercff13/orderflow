package com.univesp.orderflow.dto;

import com.univesp.orderflow.model.enums.UserRole;

import java.util.UUID;

public record UserDTO(UUID id, String name, String email, UserRole role) {
}
