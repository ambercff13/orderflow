package com.univesp.orderflow.controller;

import com.univesp.orderflow.dto.LoginDTO;
import com.univesp.orderflow.dto.LoginResponseDTO;
import com.univesp.orderflow.dto.RefreshTokenDTO;
import com.univesp.orderflow.dto.RegisterDTO;
import com.univesp.orderflow.dto.UserDTO;
import com.univesp.orderflow.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public UserDTO register(@Valid @RequestBody RegisterDTO data) {
        return authService.register(data);
    }

    @PostMapping("/login")
    public LoginResponseDTO login(@Valid @RequestBody LoginDTO data) {
        return authService.login(data);
    }

    @PostMapping("/refresh")
    public LoginResponseDTO refresh(@Valid @RequestBody RefreshTokenDTO data) {
        return authService.refresh(data);
    }

    @GetMapping("/me")
    public UserDTO currentUser(Authentication authentication) {
        return authService.currentUser(authentication);
    }
}
