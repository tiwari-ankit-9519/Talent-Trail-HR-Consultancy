package com.abchr.platform.auth;

import com.abchr.platform.audit.AuditActions;
import com.abchr.platform.audit.AuditOutcome;
import com.abchr.platform.audit.AuditService;
import com.abchr.platform.shared.config.AppProperties;
import com.abchr.platform.shared.web.ApiException;
import com.abchr.platform.shared.web.ErrorCodes;
import com.abchr.platform.shared.web.RequestInfo;
import com.abchr.platform.user.AppUser;
import com.abchr.platform.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository repository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final AppProperties properties;

    public RefreshTokenService(
            RefreshTokenRepository repository,
            UserRepository userRepository,
            AuditService auditService,
            AppProperties properties) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.properties = properties;
    }

    public record Issued(String rawToken, Duration ttl) {
    }

    public record Rotated(AppUser user, Issued issued) {
    }

    public Duration ttl() {
        return Duration.ofDays(properties.security().jwtRefreshTtlDays());
    }

    @Transactional
    public Issued issueNewFamily(UUID userId) {
        return issue(userId, UUID.randomUUID());
    }

    @Transactional(noRollbackFor = ApiException.class)
    public Rotated rotate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw invalid();
        }
        Instant now = Instant.now();
        RefreshToken current = repository.findByTokenHash(hash(rawToken)).orElseThrow(this::invalid);
        if (current.isRevoked()) {
            repository.revokeFamily(current.getFamilyId(), now);
            String email = userRepository.findById(current.getUserId()).map(AppUser::getEmail).orElse("unknown");
            auditService.recordAs(current.getUserId(), email, AuditActions.TOKEN_REUSE_DETECTED,
                    AuditActions.ENTITY_SESSION, current.getUserId().toString(),
                    "A revoked refresh token was presented again; every session in that family was revoked",
                    null, AuditOutcome.FAILURE);
            throw invalid();
        }
        if (current.isExpiredAt(now)) {
            throw invalid();
        }
        AppUser user = userRepository.findById(current.getUserId()).orElseThrow(this::invalid);
        if (!user.isActive()) {
            repository.revokeFamily(current.getFamilyId(), now);
            throw invalid();
        }
        current.revoke(now);
        return new Rotated(user, issue(user.getId(), current.getFamilyId()));
    }

    @Transactional
    public AppUser revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return null;
        }
        return repository.findByTokenHash(hash(rawToken)).map(token -> {
            repository.revokeFamily(token.getFamilyId(), Instant.now());
            return userRepository.findById(token.getUserId()).orElse(null);
        }).orElse(null);
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        repository.revokeAllForUser(userId, Instant.now());
    }

    @Scheduled(cron = "0 15 3 * * *")
    @Transactional
    public void purgeExpired() {
        repository.deleteExpiredBefore(Instant.now().minus(30, ChronoUnit.DAYS));
    }

    private Issued issue(UUID userId, UUID familyId) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Duration ttl = ttl();
        RequestInfo request = RequestInfo.current();
        repository.save(new RefreshToken(
                userId, familyId, hash(raw), Instant.now().plus(ttl), request.ipAddress(), cut(request.userAgent())));
        return new Issued(raw, ttl);
    }

    private ApiException invalid() {
        return ApiException.unauthorized(ErrorCodes.AUTH_REFRESH_INVALID, "Your session has expired. Sign in again.");
    }

    private static String cut(String value) {
        return value == null || value.length() <= 300 ? value : value.substring(0, 300);
    }

    static String hash(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
