package com.univesp.orderflow.service.impl;

import com.univesp.orderflow.dto.LoginDTO;
import com.univesp.orderflow.dto.LoginResponseDTO;
import com.univesp.orderflow.dto.RefreshTokenDTO;
import com.univesp.orderflow.dto.RegisterDTO;
import com.univesp.orderflow.dto.UserDTO;
import com.univesp.orderflow.exceptions.ApiErrorException;
import com.univesp.orderflow.model.User;
import com.univesp.orderflow.model.enums.UserRole;
import com.univesp.orderflow.repository.UserRepository;
import com.univesp.orderflow.service.AuthService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

@Service
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;
    private final long expirationSeconds;
    private final long refreshExpirationSeconds;
    private final JwtDecoder refreshJwtDecoder;

    public AuthServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtEncoder jwtEncoder,
            @Qualifier("refreshJwtDecoder") JwtDecoder refreshJwtDecoder,
            @Value("${spring.security.jwt.expiration-seconds}") long expirationSeconds,
            @Value("${spring.security.jwt.refresh-expiration-seconds}") long refreshExpirationSeconds
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtEncoder = jwtEncoder;
        this.refreshJwtDecoder = refreshJwtDecoder;
        this.expirationSeconds = expirationSeconds;
        this.refreshExpirationSeconds = refreshExpirationSeconds;
    }

    @Override
    public UserDTO register(RegisterDTO data) {
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

    @Override
    public LoginResponseDTO login(LoginDTO data) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(data.email().trim().toLowerCase(Locale.ROOT), data.password())
            );
        } catch (AuthenticationException exception) {
            throw new ApiErrorException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos.");
        }

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ApiErrorException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos."));
        return createTokenResponse(user);
    }

    @Override
    public LoginResponseDTO refresh(RefreshTokenDTO data) {
        Jwt refreshToken;
        try {
            refreshToken = refreshJwtDecoder.decode(data.refreshToken());
        } catch (JwtException exception) {
            throw new ApiErrorException(HttpStatus.UNAUTHORIZED, "Refresh token inválido ou expirado.");
        }

        if (!"refresh".equals(refreshToken.getClaimAsString("token_type"))
                || !"orderflow".equals(refreshToken.getClaimAsString("iss"))) {
            throw new ApiErrorException(HttpStatus.UNAUTHORIZED, "Refresh token inválido ou expirado.");
        }

        User user = userRepository.findByEmail(refreshToken.getSubject())
                .orElseThrow(() -> new ApiErrorException(HttpStatus.UNAUTHORIZED, "Refresh token inválido ou expirado."));
        return createTokenResponse(user);
    }

    @Override
    public UserDTO currentUser(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ApiErrorException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
        return toDTO(user);
    }

    private UserDTO toDTO(User user) {
        return new UserDTO(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }

    private LoginResponseDTO createTokenResponse(User user) {
        String accessToken = createToken(user, "access", expirationSeconds);
        String refreshToken = createToken(user, "refresh", refreshExpirationSeconds);
        return new LoginResponseDTO(
                accessToken,
                "Bearer",
                expirationSeconds,
                refreshToken,
                refreshExpirationSeconds,
                toDTO(user)
        );
    }

    private String createToken(User user, String tokenType, long lifetimeSeconds) {
        Instant issuedAt = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("orderflow")
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(lifetimeSeconds, ChronoUnit.SECONDS))
                .subject(user.getEmail())
                .claim("roles", List.of(user.getRole().name()))
                .claim("token_type", tokenType)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
