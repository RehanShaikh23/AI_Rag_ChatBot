package com.ragchatbot.service;

import com.ragchatbot.dto.auth.LoginResponse;
import com.ragchatbot.dto.auth.RegisterRequest;
import com.ragchatbot.entity.User;
import com.ragchatbot.repository.UserRepository;
import com.ragchatbot.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService — registration, duplicate email detection,
 * and profile retrieval.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("encoded-password")
                .displayName("Test User")
                .role("USER")
                .build();
    }

    @Test
    @DisplayName("Register — succeeds with new email")
    void registerSuccess() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("new@example.com");
        request.setPassword("password123");
        request.setDisplayName("New User");

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(2L);
            return u;
        });
        when(jwtTokenProvider.generateToken("new@example.com")).thenReturn("jwt-token");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);

        LoginResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals("New User", response.getUser().getDisplayName());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Register — fails with duplicate email")
    void registerDuplicateEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setDisplayName("Dup User");

        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("getProfile — returns user profile for valid email")
    void getProfileSuccess() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        LoginResponse.UserProfile profile = authService.getProfile("test@example.com");

        assertEquals(1L, profile.getId());
        assertEquals("test@example.com", profile.getEmail());
        assertEquals("Test User", profile.getDisplayName());
        assertEquals("USER", profile.getRole());
    }

    @Test
    @DisplayName("getProfile — throws for unknown email")
    void getProfileNotFound() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> authService.getProfile("unknown@example.com"));
    }

    @Test
    @DisplayName("getUserByEmail — returns user entity")
    void getUserByEmailSuccess() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        User user = authService.getUserByEmail("test@example.com");

        assertEquals(1L, user.getId());
        assertEquals("test@example.com", user.getEmail());
    }
}
