package com.school_guardian.ms_iam.domain.port.out;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface TokenProvider {
    String generatedAccessToken(UUID profileId, UUID personId, String email, Byte roleId, String roleName,
                                String identificationNumber, UUID campusId, UUID schoolId,
                                List<String> permissions, Map<String, Object> extractClaims);
    String generatedRefreshToken(UUID profileId, UUID personId, String email, Byte roleId, String roleName,
                                 String identificationNumber, UUID campusId, UUID schoolId);
    TokenClaims parseAccessToken(String token);
    RefreshClaims parseRefreshToken(String token);
    UUID extractJti(String token);
    Instant extractExpiration(String token);
    boolean isTokenExpired(String token);

    /**
     * Time-to-live applied to newly issued access tokens. The use cases (login, refresh)
     * reuse it so the expiry they report matches the one inside the token itself.
     */
    Duration accessTokenValidity();

    /** Time-to-live applied to newly issued refresh tokens. */
    Duration refreshTokenValidity();

    record TokenClaims(UUID profileId, UUID personId, String email, Byte roleId, String roleName,
                       String identificationNumber, UUID campusId, UUID schoolId,
                       List<String> permissions, String jti, Instant issuedAt, Instant expiresAt,
                       Map<String, Object> extraClaims) {}
    record RefreshClaims(UUID profileId, UUID personId, String email, Byte roleId, UUID campusId, UUID schoolId, Instant issuedAt, Instant expiresAt) {}
}
