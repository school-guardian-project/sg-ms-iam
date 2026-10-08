package com.school_guardian.ms_iam.application.usecase;

import com.school_guardian.ms_iam.application.dto.AuthenticationData;
import com.school_guardian.ms_iam.domain.model.Profile;
import com.school_guardian.ms_iam.domain.model.Role;
import com.school_guardian.ms_iam.domain.port.in.AuthenticationRepository;
import com.school_guardian.ms_iam.domain.port.in.RefreshTokenUseCase;
import com.school_guardian.ms_iam.domain.port.out.TokenProvider;
import com.school_guardian.ms_iam.shared.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService implements RefreshTokenUseCase {

    private static final int REFRESH_TOKEN_DAYS = 30;
    private static final int ACCESS_TOKEN_MINUTES = 15;

    private final AuthenticationRepository authenticationRepository;
    private final TokenProvider tokenProvider;

    @Override
    public RefreshTokenUseCase.RefreshResponse execute(RefreshTokenUseCase.Refresh refresh) {
        var refreshClaims = tokenProvider.parseRefreshToken(refresh.refreshToken());

        if (refreshClaims.expiresAt().isBefore(Instant.now())) {
            throw new InvalidRefreshTokenException("Refresh token expired");
        }

        var authData = authenticationRepository.findByProfileId(refreshClaims.profileId())
            .orElseThrow(() -> new InvalidRefreshTokenException("Profile not found"));

        Profile profile = toProfile(authData);

        if (profile.getStatus() != Status.Active) {
            throw new AccountInactiveException("Account is inactive");
        }

        String newAccessJti = UUID.randomUUID().toString();
        String newAccessToken = tokenProvider.generatedAccessToken(
            profile.getId(), profile.getPersonId(), authData.email,
            profile.getRole().getId(), authData.campusId, authData.schoolId,
            Map.of("jti", newAccessJti)
        );

        String newRefreshToken = tokenProvider.generatedRefreshToken(
            profile.getId(), profile.getPersonId(), authData.email,
            profile.getRole().getId(), authData.campusId, authData.schoolId
        );

        Instant accessExpiresAt = Instant.now().plusSeconds(ACCESS_TOKEN_MINUTES * 60);
        Instant refreshExpiresAt = Instant.now().plusSeconds(REFRESH_TOKEN_DAYS * 24 * 60 * 60);

        return new RefreshTokenUseCase.RefreshResponse(newAccessToken, accessExpiresAt, newRefreshToken, refreshExpiresAt);
    }

    private Profile toProfile(AuthenticationData data) {
        Profile profile = new Profile();
        profile.setId(data.profileId);
        profile.setPersonId(data.personId);
        profile.setPasswordHash(data.passwordHash);
        Role role = new Role();
        role.setId(data.roleId);
        profile.setRole(role);
        profile.setStatus(Status.valueOf(data.status));
        return profile;
    }

    public static class InvalidRefreshTokenException extends RuntimeException {
        public InvalidRefreshTokenException(String message) { super(message); }
    }
    public static class AccountInactiveException extends RuntimeException {
        public AccountInactiveException(String message) { super(message); }
    }
}