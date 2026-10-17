package com.abchr.platform.shared.security;

import java.util.Arrays;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public final class Permissions {

    private Permissions() {
    }

    public static boolean hasAny(String... codes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (Arrays.asList(codes).contains(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
