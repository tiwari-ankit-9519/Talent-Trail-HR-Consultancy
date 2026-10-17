package com.abchr.platform.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class RoleDtos {

    private RoleDtos() {
    }

    public record RoleResponse(
            UUID id,
            String name,
            String description,
            boolean systemRole,
            List<String> permissions,
            long userCount,
            Long version) {
    }

    public record SaveRoleRequest(
            @NotBlank @Size(max = 80) String name,
            @Size(max = 255) String description,
            @NotNull Set<String> permissions,
            Long version) {
    }

    public record PermissionResponse(String code, String description) {
    }
}
