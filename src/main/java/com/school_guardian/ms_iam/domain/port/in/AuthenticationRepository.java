package com.school_guardian.ms_iam.domain.port.in;

import com.school_guardian.ms_iam.application.dto.AuthenticationData;

import java.util.Optional;
import java.util.UUID;

public interface AuthenticationRepository {
    Optional<AuthenticationData> findByEmail(String email);
    Optional<AuthenticationData> findByProfileId(UUID profileId);
    void updatePassword(UUID profileId, String newPasswordHash);
}