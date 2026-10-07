package com.school_guardian.ms_iam.domain.port.out;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public interface TokenProvider {
    String generatedAccessToken(UUID profileId, UUID personId, String email, Byte roleId, UUID campusId, UUID schoolId, Map<String, Object> extractClaims);
    String generatedRefreshToken(UUID profileId, UUID personId, String email, Byte roleId, UUID campusId, UUID schoolId);
    TokenClaims parseAccessToken(String token);
    RefreshClaims parseRefreshToken(String token);
    UUID extractJti(String token);
    Instant extractExpiration(String token);
    boolean isTokenExpired(String token);

    record TokenClaims(UUID profileId, UUID personId, String email, Byte roleId, UUID campusId, UUID schoolId, String jti, Instant issuedAt, Instant expiresAt, Map<String, Object> extraClaims) {}
    record RefreshClaims(UUID profileId, UUID personId, String email, Byte roleId, UUID campusId, UUID schoolId, Instant issuedAt, Instant expiresAt) {}
}