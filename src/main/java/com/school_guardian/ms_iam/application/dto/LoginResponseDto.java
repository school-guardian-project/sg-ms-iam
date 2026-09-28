package com.school_guardian.ms_iam.application.dto;

import java.time.Instant;
import java.util.UUID;

public record LoginResponseDto(
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt,
        UUID profileId,
        UUID personId,
        String email,
        Byte roleId,
        UUID campusId
) {
}
