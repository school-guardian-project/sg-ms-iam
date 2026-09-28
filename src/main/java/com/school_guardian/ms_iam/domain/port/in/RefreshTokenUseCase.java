package com.school_guardian.ms_iam.domain.port.in;

import java.time.Instant;

public interface RefreshTokenUseCase {
    RefreshResponse execute(Refresh refresh);

    record Refresh(String refreshToken) {}
    record RefreshResponse(String accessToken, Instant accessTokenExpiresAt, String refreshToken, Instant refreshTokenExpiresAt) {}
}
