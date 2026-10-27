package com.univesp.orderflow.service.impl;

import com.univesp.orderflow.dto.LoginDTO;
import com.univesp.orderflow.dto.RefreshTokenDTO;
import com.univesp.orderflow.dto.RegisterDTO;
import com.univesp.orderflow.exceptions.ApiErrorException;
import com.univesp.orderflow.model.User;
import com.univesp.orderflow.model.enums.UserRole;
import com.univesp.orderflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtEncoder jwtEncoder;
    @Mock
    private JwtDecoder refreshJwtDecoder;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                userRepository,
                passwordEncoder,
                authenticationManager,
                jwtEncoder,
                refreshJwtDecoder,
                900,
                7200
        );
    }

    @Test
    void registerNormalizesEmailAndStoresEncodedPasswordAsClient() {
        RegisterDTO data = new RegisterDTO("Ana", " ANA@EXAMPLE.COM ", "password123");
        when(userRepository.existsByEmail("ana@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        var result = authService.register(data);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User savedUser = captor.getValue();
        assertEquals("ana@example.com", savedUser.getEmail());
        assertEquals("encoded-password", savedUser.getPassword());
        assertEquals(UserRole.CLIENT, savedUser.getRole());
        assertEquals("Ana", result.name());
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmail("ana@example.com")).thenReturn(true);

        ApiErrorException exception = assertThrows(
                ApiErrorException.class,
                () -> authService.register(new RegisterDTO("Ana", "ana@example.com", "password123"))
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(userRepository, never()).save(any());
    }

    @Test
    void loginReturnsAccessAndRefreshTokens() {
        User user = user("ana@example.com");
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(UsernamePasswordAuthenticationToken.authenticated("ana@example.com", null, java.util.List.of()));
        when(userRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(user));
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenAnswer(invocation -> encodedJwt());

        var response = authService.login(new LoginDTO(" ANA@EXAMPLE.COM ", "password123"));

        assertEquals("Bearer", response.tokenType());
        assertEquals(900, response.expiresIn());
        assertEquals(7200, response.refreshExpiresIn());
        assertEquals(user.getId(), response.user().id());
        assertNotNull(response.accessToken());
        assertNotNull(response.refreshToken());
        verify(authenticationManager).authenticate(argThat(token ->
                "ana@example.com".equals(token.getPrincipal()) && "password123".equals(token.getCredentials())
        ));
    }

    @Test
    void loginRejectsInvalidCredentials() {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        ApiErrorException exception = assertThrows(
                ApiErrorException.class,
                () -> authService.login(new LoginDTO("ana@example.com", "wrong"))
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void refreshRejectsInvalidJwt() {
        when(refreshJwtDecoder.decode("invalid")).thenThrow(new BadJwtException("Invalid token"));

        ApiErrorException exception = assertThrows(
                ApiErrorException.class,
                () -> authService.refresh(new RefreshTokenDTO("invalid"))
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }

    @Test
    void refreshRejectsAccessToken() {
        Jwt accessToken = Jwt.withTokenValue("access")
                .header("alg", "HS256")
                .issuer("orderflow")
                .subject("ana@example.com")
                .claim("token_type", "access")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(900))
                .build();
        when(refreshJwtDecoder.decode("access")).thenReturn(accessToken);

        ApiErrorException exception = assertThrows(
                ApiErrorException.class,
                () -> authService.refresh(new RefreshTokenDTO("access"))
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void refreshIssuesNewTokensForValidRefreshToken() {
        User user = user("ana@example.com");
        Jwt refreshToken = Jwt.withTokenValue("refresh")
                .header("alg", "HS256")
                .issuer("orderflow")
                .subject("ana@example.com")
                .claim("token_type", "refresh")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(7200))
                .build();
        when(refreshJwtDecoder.decode("valid-refresh")).thenReturn(refreshToken);
        when(userRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(user));
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenAnswer(invocation -> encodedJwt());

        var response = authService.refresh(new RefreshTokenDTO("valid-refresh"));

        assertEquals(900, response.expiresIn());
        assertEquals(7200, response.refreshExpiresIn());
        assertEquals(user.getId(), response.user().id());
        verify(jwtEncoder, times(2)).encode(any(JwtEncoderParameters.class));
    }

    @Test
    void currentUserReturnsUserDetails() {
        User user = user("ana@example.com");
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated("ana@example.com", null, java.util.List.of());
        when(userRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(user));

        var result = authService.currentUser(authentication);

        assertEquals(user.getId(), result.id());
        assertEquals("Ana", result.name());
        assertEquals("ana@example.com", result.email());
    }

    @Test
    void currentUserReturnsNotFoundWhenUserDoesNotExist() {
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated("missing@example.com", null, java.util.List.of());
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        ApiErrorException exception = assertThrows(
                ApiErrorException.class,
                () -> authService.currentUser(authentication)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    private User user(String email) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setName("Ana");
        user.setEmail(email);
        user.setRole(UserRole.CLIENT);
        return user;
    }

    private Jwt encodedJwt() {
        Instant now = Instant.now();
        return Jwt.withTokenValue("encoded-token")
                .header("alg", "HS256")
                .subject("ana@example.com")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(900))
                .build();
    }
}
