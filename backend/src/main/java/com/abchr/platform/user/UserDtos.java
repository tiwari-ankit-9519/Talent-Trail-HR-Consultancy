package com.abchr.platform.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class UserDtos {

    private UserDtos() {
    }

    public record RoleRef(UUID id, String name) {
    }

    public record UserResponse(
            UUID id,
            String email,
            String fullName,
            UserStatus status,
            boolean mustChangePassword,
            boolean locked,
            Instant lastLoginAt,
            List<RoleRef> roles,
            Long version,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record CreateUserRequest(
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(max = 150) String fullName,
            @NotBlank @Size(max = 128) String password,
            @NotEmpty Set<UUID> roleIds) {
    }

    public record UpdateUserRequest(
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(max = 150) String fullName,
            @NotEmpty Set<UUID> roleIds,
            Long version) {
    }

    public record StatusRequest(boolean active) {
    }

    public record ResetPasswordRequest(@NotBlank @Size(max = 128) String password) {
    }
}
