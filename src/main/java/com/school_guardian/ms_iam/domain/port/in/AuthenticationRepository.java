package com.school_guardian.ms_iam.domain.port.in;

import com.school_guardian.ms_iam.application.dto.AuthenticationData;
import com.school_guardian.ms_iam.application.dto.ProfileView;
import com.school_guardian.ms_iam.infrastructure.persistence.entity.RefreshTokenEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuthenticationRepository {
    Optional<AuthenticationData> findByPersonId(UUID personId);
    Optional<AuthenticationData> findByEmail(String email);
    Optional<AuthenticationData> findByProfileId(UUID profileId);
    Optional<ProfileView> findProfileViewById(UUID profileId);
    List<String> findPermissionNamesByRoleId(Byte roleId);

    // Refresh Token Management
    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);
    void saveRefreshToken(RefreshTokenEntity token);
    void saveRefreshToken(RefreshTokenEntity newToken, RefreshTokenEntity oldToken);
    void revokeAllTokensInSession(Integer sessionProfileId);
}