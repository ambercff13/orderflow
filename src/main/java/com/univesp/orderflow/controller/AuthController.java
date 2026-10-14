package com.univesp.orderflow.controller;

import com.univesp.orderflow.dto.LoginDTO;
import com.univesp.orderflow.dto.RegisterDTO;
import com.univesp.orderflow.dto.UserDTO;
import com.univesp.orderflow.exceptions.ApiErrorException;
import com.univesp.orderflow.model.User;
import com.univesp.orderflow.model.enums.UserRole;
import com.univesp.orderflow.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    @GetMapping("/csrf")
    public CsrfToken csrfToken(CsrfToken csrfToken) {
        return csrfToken;
    }

    @PostMapping("/register")
    public UserDTO register(@Valid @RequestBody RegisterDTO data) {
        String email = data.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            throw new ApiErrorException(HttpStatus.CONFLICT, "Este e-mail já está cadastrado.");
        }

        User user = new User();
        user.setName(data.name());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(data.password()));
        user.setRole(UserRole.CLIENT);
        return toDTO(userRepository.save(user));
    }

    @PostMapping("/login")
    public UserDTO login(
            @Valid @RequestBody LoginDTO data,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(data.email().trim().toLowerCase(Locale.ROOT), data.password())
            );
        } catch (AuthenticationException exception) {
            throw new ApiErrorException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos.");
        }

        request.getSession(true);
        request.changeSessionId();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return toDTO(user);
    }

    @PostMapping("/logout")
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        new SecurityContextLogoutHandler().logout(request, response, authentication);
    }

    @GetMapping("/me")
    public UserDTO currentUser(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ApiErrorException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
        return toDTO(user);
    }

    private UserDTO toDTO(User user) {
        return new UserDTO(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}
