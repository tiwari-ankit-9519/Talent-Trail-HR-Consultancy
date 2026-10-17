package com.abchr.platform.auth;

import com.abchr.platform.auth.AuthDtos.AuthResult;
import com.abchr.platform.auth.AuthDtos.ChangePasswordRequest;
import com.abchr.platform.auth.AuthDtos.LoginRequest;
import com.abchr.platform.auth.AuthDtos.SessionResponse;
import com.abchr.platform.auth.AuthDtos.UserProfile;
import com.abchr.platform.shared.config.AppProperties;
import com.abchr.platform.shared.web.ApiException;
import com.abchr.platform.shared.web.ErrorCodes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    static final String REFRESH_COOKIE = "abchr_rt";
    private static final String COOKIE_PATH = "/api/v1/auth";
    private static final String GUARD_HEADER = "X-Requested-With";
    private static final String GUARD_VALUE = "XMLHttpRequest";

    private final AuthService authService;
    private final AppProperties properties;

    public AuthController(AuthService authService, AppProperties properties) {
        this.authService = authService;
        this.properties = properties;
    }

    @PostMapping("/login")
    public ResponseEntity<SessionResponse> login(@Valid @RequestBody LoginRequest request) {
        return withCookie(authService.login(request.email(), request.password()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<SessionResponse> refresh(
            HttpServletRequest http,
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        requireGuardHeader(http);
        return withCookie(authService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest http,
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        requireGuardHeader(http);
        authService.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString())
                .build();
    }

    @GetMapping("/me")
    public UserProfile me() {
        return authService.me();
    }

    @PostMapping("/change-password")
    public ResponseEntity<SessionResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        return withCookie(authService.changePassword(request.currentPassword(), request.newPassword()));
    }

    private ResponseEntity<SessionResponse> withCookie(AuthResult result) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        cookie(result.refresh().rawToken(), result.refresh().ttl()).toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(result.session());
    }

    private ResponseCookie cookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(properties.auth().cookieSecure())
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(maxAge)
                .build();
    }

    private static void requireGuardHeader(HttpServletRequest request) {
        if (!GUARD_VALUE.equals(request.getHeader(GUARD_HEADER))) {
            throw ApiException.forbidden(ErrorCodes.FORBIDDEN, "Missing request guard header");
        }
    }
}
