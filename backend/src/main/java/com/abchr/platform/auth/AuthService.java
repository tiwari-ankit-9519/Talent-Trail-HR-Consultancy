package com.abchr.platform.auth;

import com.abchr.platform.audit.AuditActions;
import com.abchr.platform.audit.AuditOutcome;
import com.abchr.platform.audit.AuditService;
import com.abchr.platform.auth.AuthDtos.AuthResult;
import com.abchr.platform.auth.AuthDtos.SessionResponse;
import com.abchr.platform.auth.AuthDtos.UserProfile;
import com.abchr.platform.security.JwtService;
import com.abchr.platform.shared.config.AppProperties;
import com.abchr.platform.shared.security.Actor;
import com.abchr.platform.shared.web.ApiException;
import com.abchr.platform.shared.web.ErrorCodes;
import com.abchr.platform.user.AppUser;
import com.abchr.platform.user.UserRepository;
import java.time.Instant;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuditService auditService;
    private final AppProperties properties;
    private final String dummyHash;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            AuditService auditService,
            AppProperties properties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.auditService = auditService;
        this.properties = properties;
        this.dummyHash = passwordEncoder.encode("dummy-password-for-timing");
    }

    @Transactional(noRollbackFor = ApiException.class)
    public AuthResult login(String rawEmail, String password) {
        String email = rawEmail.trim().toLowerCase();
        Instant now = Instant.now();
        AppUser user = userRepository.findByEmailIgnoreCase(email).orElse(null);

        if (user == null) {
            passwordEncoder.matches(password, dummyHash);
            auditSignInFailure(null, email, "Failed sign-in: unknown email");
            throw invalidCredentials();
        }

        user.clearExpiredLockout(now);
        if (user.isLockedAt(now)) {
            auditSignInFailure(user, email, "Sign-in blocked: account is locked");
            throw ApiException.locked(ErrorCodes.AUTH_ACCOUNT_LOCKED,
                    "Too many failed attempts. Try again later.");
        }
        if (!user.isActive()) {
            passwordEncoder.matches(password, dummyHash);
            auditSignInFailure(user, email, "Sign-in blocked: account is inactive");
            throw invalidCredentials();
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            boolean locked = user.registerFailedLogin(
                    properties.auth().maxFailedAttempts(), properties.auth().lockMinutes(), now);
            userRepository.save(user);
            auditSignInFailure(user, email, "Failed sign-in: wrong password");
            if (locked) {
                auditService.recordAs(user.getId(), email, AuditActions.ACCOUNT_LOCKED, AuditActions.ENTITY_USER,
                        user.getId().toString(),
                        "Account locked for " + properties.auth().lockMinutes() + " minutes after repeated failures",
                        null, AuditOutcome.FAILURE);
            }
            throw invalidCredentials();
        }

        user.registerSuccessfulLogin(now);
        userRepository.save(user);
        auditService.recordAs(user.getId(), email, AuditActions.LOGIN_SUCCESS, AuditActions.ENTITY_SESSION,
                user.getId().toString(), "Signed in", null, AuditOutcome.SUCCESS);
        return result(user, refreshTokenService.issueNewFamily(user.getId()));
    }

    @Transactional(noRollbackFor = ApiException.class)
    public AuthResult refresh(String rawRefreshToken) {
        RefreshTokenService.Rotated rotated = refreshTokenService.rotate(rawRefreshToken);
        return result(rotated.user(), rotated.issued());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        AppUser user = refreshTokenService.revoke(rawRefreshToken);
        if (user != null) {
            auditService.recordAs(user.getId(), user.getEmail(), AuditActions.LOGOUT, AuditActions.ENTITY_SESSION,
                    user.getId().toString(), "Signed out", null, AuditOutcome.SUCCESS);
        }
    }

    @Transactional(readOnly = true)
    public UserProfile me() {
        return profile(currentUser());
    }

    @Transactional(noRollbackFor = ApiException.class)
    public AuthResult changePassword(String currentPassword, String newPassword) {
        AppUser user = currentUser();
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            auditService.recordAs(user.getId(), user.getEmail(), AuditActions.PASSWORD_CHANGED,
                    AuditActions.ENTITY_USER, user.getId().toString(),
                    "Password change failed: current password was wrong", null, AuditOutcome.FAILURE);
            throw ApiException.rule(ErrorCodes.AUTH_CURRENT_PASSWORD_WRONG, "The current password is incorrect");
        }
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw ApiException.rule(ErrorCodes.AUTH_PASSWORD_UNCHANGED,
                    "The new password must be different from the current one");
        }
        PasswordPolicy.validate(newPassword, user.getEmail());

        user.setPassword(passwordEncoder.encode(newPassword), false, Instant.now());
        userRepository.save(user);
        refreshTokenService.revokeAllForUser(user.getId());
        auditService.recordAs(user.getId(), user.getEmail(), AuditActions.PASSWORD_CHANGED, AuditActions.ENTITY_USER,
                user.getId().toString(), "Changed own password", null, AuditOutcome.SUCCESS);
        return result(user, refreshTokenService.issueNewFamily(user.getId()));
    }

    private AppUser currentUser() {
        Actor actor = Actor.current();
        if (actor.id() == null) {
            throw ApiException.unauthorized(ErrorCodes.UNAUTHENTICATED, "Authentication is required");
        }
        return userRepository.findById(actor.id())
                .filter(AppUser::isActive)
                .orElseThrow(() -> ApiException.unauthorized(ErrorCodes.UNAUTHENTICATED, "Authentication is required"));
    }

    private AuthResult result(AppUser user, RefreshTokenService.Issued refresh) {
        JwtService.AccessToken token = jwtService.issue(user);
        return new AuthResult(new SessionResponse(token.value(), token.expiresInSeconds(), profile(user)), refresh);
    }

    private UserProfile profile(AppUser user) {
        return new UserProfile(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.roleNames(),
                user.permissionCodes().stream().sorted().toList(),
                user.isMustChangePassword());
    }

    private void auditSignInFailure(AppUser user, String email, String summary) {
        auditService.recordAs(
                user == null ? null : user.getId(),
                email,
                AuditActions.LOGIN_FAILED,
                AuditActions.ENTITY_SESSION,
                user == null ? null : user.getId().toString(),
                summary,
                null,
                AuditOutcome.FAILURE);
    }

    private ApiException invalidCredentials() {
        return ApiException.unauthorized(ErrorCodes.AUTH_INVALID_CREDENTIALS, "Email or password is incorrect");
    }
}
