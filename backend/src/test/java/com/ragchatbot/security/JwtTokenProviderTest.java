package com.ragchatbot.security;

import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JwtTokenProvider — validates token generation,
 * parsing, and rejection of invalid/expired tokens.
 */
class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;

    // A 256-bit+ hex secret for HMAC-SHA256
    private static final String TEST_SECRET =
            "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2";

    @BeforeEach
    void setUp() {
        // 1-hour expiration for normal tests
        tokenProvider = new JwtTokenProvider(TEST_SECRET, 3_600_000L);
    }

    @Test
    @DisplayName("Generate token and extract email (subject)")
    void generateAndExtractEmail() {
        String email = "user@example.com";
        String token = tokenProvider.generateToken(email);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertEquals(email, tokenProvider.getEmailFromToken(token));
    }

    @Test
    @DisplayName("Valid token passes validation")
    void validateValidToken() {
        String token = tokenProvider.generateToken("user@example.com");
        assertTrue(tokenProvider.validateToken(token));
    }

    @Test
    @DisplayName("Tampered token fails validation")
    void validateTamperedToken() {
        String token = tokenProvider.generateToken("user@example.com");
        // Flip a character in the signature part
        String tampered = token.substring(0, token.length() - 2) + "XX";
        assertFalse(tokenProvider.validateToken(tampered));
    }

    @Test
    @DisplayName("Empty/null token fails validation")
    void validateEmptyToken() {
        assertFalse(tokenProvider.validateToken(""));
        assertFalse(tokenProvider.validateToken("   "));
    }

    @Test
    @DisplayName("Expired token fails validation")
    void validateExpiredToken() {
        // Create provider with 0ms expiration (token expires immediately)
        JwtTokenProvider expiredProvider = new JwtTokenProvider(TEST_SECRET, 0L);
        String token = expiredProvider.generateToken("user@example.com");

        // Small delay to ensure expiration
        try { Thread.sleep(10); } catch (InterruptedException ignored) {}

        assertFalse(expiredProvider.validateToken(token));
    }

    @Test
    @DisplayName("Token signed with different secret fails validation")
    void validateDifferentSecret() {
        String otherSecret = "f0e1d2c3b4a5f6e7d8c9b0a1f2e3d4c5b6a7f8e9d0c1b2a3f4e5d6c7b8a9f0e1";
        JwtTokenProvider otherProvider = new JwtTokenProvider(otherSecret, 3_600_000L);

        String token = otherProvider.generateToken("user@example.com");
        assertFalse(tokenProvider.validateToken(token));
    }

    @Test
    @DisplayName("getExpirationMs returns configured value")
    void expirationMs() {
        assertEquals(3_600_000L, tokenProvider.getExpirationMs());
    }
}
