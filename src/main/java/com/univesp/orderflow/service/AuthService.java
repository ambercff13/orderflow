package com.univesp.orderflow.service;

import com.univesp.orderflow.dto.LoginDTO;
import com.univesp.orderflow.dto.LoginResponseDTO;
import com.univesp.orderflow.dto.RefreshTokenDTO;
import com.univesp.orderflow.dto.RegisterDTO;
import com.univesp.orderflow.dto.UserDTO;
import org.springframework.security.core.Authentication;

public interface AuthService {
    UserDTO register(RegisterDTO data);
    LoginResponseDTO login(LoginDTO data);
    LoginResponseDTO refresh(RefreshTokenDTO data);
    UserDTO currentUser(Authentication authentication);
}
