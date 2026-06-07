package com.ragchatbot.service;

import com.ragchatbot.dto.auth.LoginRequest;
import com.ragchatbot.dto.auth.LoginResponse;
import com.ragchatbot.dto.auth.RegisterRequest;
import com.ragchatbot.entity.User;
import com.ragchatbot.repository.UserRepository;
import com.ragchatbot.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles user registration, login, and profile retrieval.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Register a new user account.
     */
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        log.info("Registering user: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .displayName(request.getDisplayName())
                .role("USER")
                .build();

        user = userRepository.save(user);
        log.info("User registered successfully: {} (id={})", user.getEmail(), user.getId());

        // Auto-login after registration
        String token = jwtTokenProvider.generateToken(user.getEmail());

        return buildLoginResponse(user, token);
    }

    /**
     * Authenticate a user and return a JWT token.
     */
    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt: {}", request.getEmail());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(), request.getPassword()));

        String token = jwtTokenProvider.generateToken(authentication);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found after authentication"));

        log.info("User logged in: {}", user.getEmail());

        return buildLoginResponse(user, token);
    }

    /**
     * Get the current user's profile from their email.
     */
    public LoginResponse.UserProfile getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        return LoginResponse.UserProfile.builder()
                .id(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .role(user.getRole())
                .build();
    }

    /**
     * Find user entity by email (used internally by other services).
     */
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    private LoginResponse buildLoginResponse(User user, String token) {
        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationMs() / 1000)
                .user(LoginResponse.UserProfile.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .displayName(user.getDisplayName())
                        .role(user.getRole())
                        .build())
                .build();
    }
}
