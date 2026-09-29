package com.school_guardian.ms_iam.application.dto;

import java.time.Instant;

public record RefreshResponseDto(
    String accessToken,
    Instant accessTokenExpiresAt,
    String refreshToken,
    Instant refreshTokenExpiresAt
) {
}