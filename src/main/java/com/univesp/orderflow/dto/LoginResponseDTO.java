package com.univesp.orderflow.dto;

public record LoginResponseDTO(
        String accessToken,
        String tokenType,
        long expiresIn,
        String refreshToken,
        long refreshExpiresIn,
        UserDTO user
) {
}
