package com.abchr.platform.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank @Size(max = 255) String email,
            @NotBlank @Size(max = 128) String password) {
    }

    public record ChangePasswordRequest(
            @NotBlank @Size(max = 128) String currentPassword,
            @NotBlank @Size(max = 128) String newPassword) {
    }

    public record UserProfile(
            UUID id,
            String email,
            String fullName,
            List<String> roles,
            List<String> permissions,
            boolean mustChangePassword) {
    }

    public record SessionResponse(String accessToken, long expiresInSeconds, UserProfile user) {
    }

    public record AuthResult(SessionResponse session, RefreshTokenService.Issued refresh) {
    }
}
