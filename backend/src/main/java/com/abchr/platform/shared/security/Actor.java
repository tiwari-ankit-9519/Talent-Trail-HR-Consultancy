package com.abchr.platform.shared.security;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

public record Actor(UUID id, String email) {

    public static final Actor SYSTEM = new Actor(null, "system");

    private static final ThreadLocal<Actor> OVERRIDE = new ThreadLocal<>();

    public static void runAs(String email, Runnable action) {
        Actor previous = OVERRIDE.get();
        OVERRIDE.set(new Actor(null, email == null || email.isBlank() ? "system" : email));
        try {
            action.run();
        } finally {
            if (previous == null) {
                OVERRIDE.remove();
            } else {
                OVERRIDE.set(previous);
            }
        }
    }

    public static Actor current() {
        Actor override = OVERRIDE.get();
        if (override != null) {
            return override;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return SYSTEM;
        }
        UUID id = null;
        try {
            id = UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException ignored) {
            id = null;
        }
        String email = jwt.getClaimAsString("email");
        return new Actor(id, email == null ? "unknown" : email);
    }
}
