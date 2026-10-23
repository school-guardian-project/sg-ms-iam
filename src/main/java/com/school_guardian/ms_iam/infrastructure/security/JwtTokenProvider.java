package com.school_guardian.ms_iam.infrastructure.security;

import com.school_guardian.ms_iam.domain.port.out.TokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Issues and verifies JWTs using RS256 (asymmetric RSA).
 *
 * <p>Access and refresh tokens are signed with the IAM private key. The matching
 * public key is published through the JWKS endpoint so the API gateway (Kong)
 * can validate tokens without sharing any secret with the services.</p>
 */
@Component
public class JwtTokenProvider implements TokenProvider {

    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final String keyId;
    private final String issuer;
    private final String audience;
    private final long accessTokenExpirationMinutes;
    private final long refreshTokenExpirationDays;

    public JwtTokenProvider(
        RsaKeyProvider keyProvider,
        @Value("${app.jwt.issuer:school-guardian-iam}") String issuer,
        @Value("${app.jwt.audience:school-guardian-api}") String audience,
        @Value("${app.jwt.access-token-expiration-minutes:15}") long accessTokenExpirationMinutes,
        @Value("${app.jwt.refresh-token-expiration-days:7}") long refreshTokenExpirationDays
    ) {
        this.privateKey = keyProvider.getPrivateKey();
        this.publicKey = keyProvider.getPublicKey();
        this.keyId = keyProvider.getKeyId();
        this.issuer = issuer;
        this.audience = audience;
        this.accessTokenExpirationMinutes = accessTokenExpirationMinutes;
        this.refreshTokenExpirationDays = refreshTokenExpirationDays;
    }

    @Override
    public String generatedAccessToken(UUID profileId, UUID personId, String email, Byte roleId, String roleName,
                                       String identificationNumber, UUID campusId, UUID schoolId,
                                       List<String> permissions, Map<String, Object> extractClaims) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(accessTokenExpirationMinutes * 60);
        String jti = (String) extractClaims.getOrDefault("jti", UUID.randomUUID().toString());

        var builder = Jwts.builder()
            .header().keyId(keyId).and()
            .issuer(issuer)
            .audience().add(audience).and()
            .subject(profileId.toString())
            .claim("personId", personId.toString())
            .claim("roleId", roleId)
            .claim("type", "access")
            .claim("jti", jti)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry));

        if (email != null) builder.claim("email", email);
        if (roleName != null) builder.claim("role", roleName);
        if (identificationNumber != null) builder.claim("documentNumber", identificationNumber);
        if (campusId != null) builder.claim("campusId", campusId.toString());
        if (schoolId != null) builder.claim("schoolId", schoolId.toString());
        builder.claim("permissions", permissions != null ? permissions : List.of());

        return builder.signWith(privateKey, Jwts.SIG.RS256).compact();
    }

    @Override
    public String generatedRefreshToken(UUID profileId, UUID personId, String email, Byte roleId, String roleName,
                                        String identificationNumber, UUID campusId, UUID schoolId) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(refreshTokenExpirationDays * 24 * 60 * 60);

        var builder = Jwts.builder()
            .header().keyId(keyId).and()
            .issuer(issuer)
            .audience().add(audience).and()
            .subject(profileId.toString())
            .claim("personId", personId.toString())
            .claim("roleId", roleId)
            .claim("type", "refresh")
            // A refresh token needs its own unique id: rotation hashes the token and
            // two tokens issued within the same second would otherwise be identical.
            .claim("jti", UUID.randomUUID().toString())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry));

        if (email != null) builder.claim("email", email);
        if (roleName != null) builder.claim("role", roleName);
        if (identificationNumber != null) builder.claim("documentNumber", identificationNumber);
        if (campusId != null) builder.claim("campusId", campusId.toString());
        if (schoolId != null) builder.claim("schoolId", schoolId.toString());

        return builder.signWith(privateKey, Jwts.SIG.RS256).compact();
    }

    @Override
    public TokenClaims parseAccessToken(String token) {
        Claims claims = parse(token);
        if (!"access".equals(claims.get("type"))) {
            throw new JwtException("Not an access token");
        }
        List<String> permissions = claims.get("permissions", List.class);
        return new TokenClaims(
            UUID.fromString(claims.getSubject()),
            UUID.fromString(claims.get("personId", String.class)),
            claims.get("email", String.class),
            claims.get("roleId", Byte.class),
            claims.get("role", String.class),
            claims.get("documentNumber", String.class),
            claims.get("campusId") != null ? UUID.fromString(claims.get("campusId", String.class)) : null,
            claims.get("schoolId") != null ? UUID.fromString(claims.get("schoolId", String.class)) : null,
            permissions != null ? permissions : List.of(),
            claims.get("jti", String.class),
            claims.getIssuedAt().toInstant(),
            claims.getExpiration().toInstant(),
            Map.of()
        );
    }

    @Override
    public RefreshClaims parseRefreshToken(String token) {
        Claims claims = parse(token);
        if (!"refresh".equals(claims.get("type"))) {
            throw new JwtException("Not a refresh token");
        }
        return new RefreshClaims(
            UUID.fromString(claims.getSubject()),
            UUID.fromString(claims.get("personId", String.class)),
            claims.get("email", String.class),
            claims.get("roleId", Byte.class),
            claims.get("campusId") != null ? UUID.fromString(claims.get("campusId", String.class)) : null,
            claims.get("schoolId") != null ? UUID.fromString(claims.get("schoolId", String.class)) : null,
            claims.get("jti", String.class),
            claims.getIssuedAt().toInstant(),
            claims.getExpiration().toInstant()
        );
    }

    private Claims parse(String token) {
        return Jwts.parser()
            .verifyWith(publicKey)
            .requireIssuer(issuer)
            .requireAudience(audience)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    @Override
    public UUID extractJti(String token) {
        return UUID.fromString(parseAccessToken(token).jti());
    }

    @Override
    public Instant extractExpiration(String token) {
        return parseAccessToken(token).expiresAt();
    }

    @Override
    public boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).isBefore(Instant.now());
        } catch (JwtException e) {
            return true;
        }
    }

    @Override
    public Duration accessTokenValidity() {
        return Duration.ofMinutes(accessTokenExpirationMinutes);
    }

    @Override
    public Duration refreshTokenValidity() {
        return Duration.ofDays(refreshTokenExpirationDays);
    }
}
