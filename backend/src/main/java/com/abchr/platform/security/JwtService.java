package com.abchr.platform.security;

import com.abchr.platform.shared.config.AppProperties;
import com.abchr.platform.user.AppUser;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    public static final String ISSUER = "abchr-platform";
    public static final String PERMISSIONS_CLAIM = "perms";

    private final JwtEncoder encoder;
    private final AppProperties properties;

    public JwtService(JwtEncoder encoder, AppProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
    }

    public record AccessToken(String value, long expiresInSeconds) {
    }

    public AccessToken issue(AppUser user) {
        Instant now = Instant.now();
        Duration ttl = Duration.ofMinutes(properties.security().jwtAccessTtlMinutes());
        List<String> permissions = user.isMustChangePassword()
                ? List.of()
                : user.permissionCodes().stream().sorted().toList();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .claim("email", user.getEmail())
                .claim("name", user.getFullName())
                .claim(PERMISSIONS_CLAIM, permissions)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AccessToken(token, ttl.toSeconds());
    }
}
