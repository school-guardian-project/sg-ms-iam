package com.school_guardian.ms_iam.infrastructure.security;

import com.school_guardian.ms_iam.domain.port.out.TokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtTokenProvider implements TokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenExpirationMinutes;
    private final long refreshTokenExpirationDays;

    public JwtTokenProvider(
        @Value("${app.jwt.secret}") String secret,
        @Value("${app.jwt.access-token-expiration-minutes:15}") long accessTokenExpirationMinutes,
        @Value("${app.jwt.refresh-token-expiration-days:30}") long refreshTokenExpirationDays
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMinutes = accessTokenExpirationMinutes;
        this.refreshTokenExpirationDays = refreshTokenExpirationDays;
    }

    @Override
    public String generatedAccessToken(UUID profileId, UUID personId, String email, Byte roleId, UUID campusId, Map<String, Object> extractClaims) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(accessTokenExpirationMinutes * 60);
        String jti = (String) extractClaims.getOrDefault("jti", UUID.randomUUID().toString());

        var builder = Jwts.builder()
            .subject(profileId.toString())
            .claim("personId", personId.toString())
            .claim("roleId", roleId)
            .claim("type", "access")
            .claim("jti", jti)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry));

        if (email != null) builder.claim("email", email);
        if (campusId != null) builder.claim("campusId", campusId.toString());

        return builder.signWith(secretKey).compact();
    }

    @Override
    public String generatedRefreshToken(UUID profileId, UUID personId, String email, Byte roleId, UUID campusId) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(refreshTokenExpirationDays * 24 * 60 * 60);

        var builder = Jwts.builder()
            .subject(profileId.toString())
            .claim("personId", personId.toString())
            .claim("roleId", roleId)
            .claim("type", "refresh")
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry));

        if (email != null) builder.claim("email", email);
        if (campusId != null) builder.claim("campusId", campusId.toString());

        return builder.signWith(secretKey).compact();
    }

    @Override
    public TokenClaims parseAccessToken(String token) {
        Claims claims = parse(token);
        if (!"access".equals(claims.get("type"))) {
            throw new JwtException("Not an access token");
        }
        return new TokenClaims(
            UUID.fromString(claims.getSubject()),
            UUID.fromString(claims.get("personId", String.class)),
            claims.get("email", String.class),
            claims.get("roleId", Byte.class),
            claims.get("campusId") != null ? UUID.fromString(claims.get("campusId", String.class)) : null,
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
            claims.getIssuedAt().toInstant(),
            claims.getExpiration().toInstant()
        );
    }

    private Claims parse(String token) {
        return Jwts.parser()
            .verifyWith(secretKey)
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
}