package com.ragchatbot.dto.auth;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class LoginResponse {

    private String token;
    private String tokenType;
    private long expiresIn;
    private UserProfile user;

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    @Builder
    public static class UserProfile {
        private Long id;
        private String email;
        private String displayName;
        private String role;
    }
}
