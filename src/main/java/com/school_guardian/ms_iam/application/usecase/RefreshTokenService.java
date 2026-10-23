package com.school_guardian.ms_iam.application.usecase;

import com.school_guardian.ms_iam.application.mapper.ProfileAuthMapper;
import com.school_guardian.ms_iam.domain.model.Profile;
import com.school_guardian.ms_iam.domain.port.in.AuthenticationRepository;
import com.school_guardian.ms_iam.domain.port.in.RefreshTokenUseCase;
import com.school_guardian.ms_iam.domain.port.out.TokenProvider;
import com.school_guardian.ms_iam.infrastructure.persistence.repository.SessionProfileJpaRepository;
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

    private final AuthenticationRepository authenticationRepository;
    private final SessionProfileJpaRepository sessionProfileRepository;
    private final TokenProvider tokenProvider;

    @Override
    public RefreshTokenUseCase.RefreshResponse execute(RefreshTokenUseCase.Refresh refresh) {
        var refreshClaims = tokenProvider.parseRefreshToken(refresh.refreshToken());

        if (refreshClaims.expiresAt().isBefore(Instant.now())) {
            throw new InvalidRefreshTokenException("Refresh token expired");
        }

        var authData = authenticationRepository.findByProfileId(refreshClaims.profileId())
            .orElseThrow(() -> new InvalidRefreshTokenException("Profile not found"));

        Profile profile = ProfileAuthMapper.toProfile(authData);

        if (profile.getStatus() != Status.Active) {
            throw new AccountInactiveException("Account is inactive");
        }

        String newAccessJti = UUID.randomUUID().toString();
        String newAccessToken = tokenProvider.generatedAccessToken(
            profile.getId(), profile.getPersonId(), authData.email,
            profile.getRole().getId(), authData.roleName, authData.identificationNumber,
            authData.campusId, authData.schoolId, authData.permissions,
            Map.of("jti", newAccessJti)
        );

        String newRefreshToken = tokenProvider.generatedRefreshToken(
            profile.getId(), profile.getPersonId(), authData.email,
            profile.getRole().getId(), authData.roleName, authData.identificationNumber,
            authData.campusId, authData.schoolId
        );

        // Same source as the one embedded in the tokens (TokenProvider <- app.jwt.*).
        Instant accessExpiresAt = Instant.now().plus(tokenProvider.accessTokenValidity());
        Instant refreshExpiresAt = Instant.now().plus(tokenProvider.refreshTokenValidity());

        // SessionProfile holds one row per login, so the session to extend is the newest
        // one for this profile (findByProfileId would throw once a second row exists).
        try {
            var session = sessionProfileRepository.findFirstByProfileIdOrderByIdDesc(authData.profileId);
            if (session != null) {
                session.setDateEnd(refreshExpiresAt);
                session.setStatusSession("Active");
                sessionProfileRepository.save(session);
            }
        } catch (Exception e) {
            log.warn("No se pudo actualizar el SessionProfile del perfil {}: {}",
                authData.profileId, e.getMessage());
        }

        return new RefreshTokenUseCase.RefreshResponse(newAccessToken, accessExpiresAt, newRefreshToken, refreshExpiresAt);
    }

    public static class InvalidRefreshTokenException extends RuntimeException {
        public InvalidRefreshTokenException(String message) { super(message); }
    }
    public static class AccountInactiveException extends RuntimeException {
        public AccountInactiveException(String message) { super(message); }
    }
}
