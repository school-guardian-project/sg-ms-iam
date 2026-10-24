package com.school_guardian.ms_iam.application.usecase;

import com.school_guardian.ms_iam.application.mapper.ProfileAuthMapper;
import com.school_guardian.ms_iam.domain.model.Profile;
import com.school_guardian.ms_iam.domain.port.in.AuthenticationRepository;
import com.school_guardian.ms_iam.domain.port.in.RefreshTokenUseCase;
import com.school_guardian.ms_iam.domain.port.out.TokenProvider;
import com.school_guardian.ms_iam.infrastructure.persistence.repository.SessionProfileJpaRepository;
import com.school_guardian.ms_iam.infrastructure.persistence.entity.RefreshTokenEntity;
import com.school_guardian.ms_iam.infrastructure.security.RefreshTokenHasher;
import com.school_guardian.ms_iam.shared.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    @Override
    public RefreshTokenUseCase.RefreshResponse execute(RefreshTokenUseCase.Refresh refresh) {
        var refreshClaims = tokenProvider.parseRefreshToken(refresh.refreshToken());
        String tokenHash = RefreshTokenHasher.sha256(refresh.refreshToken());

        if (refreshClaims.expiresAt().isBefore(Instant.now())) {
            throw new InvalidRefreshTokenException("Refresh token expired");
        }

        var tokenEntity = authenticationRepository.findByTokenHash(tokenHash)
            .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token not found"));

        if (tokenEntity.getRevokedAt() != null) {
            handleReuseDetection(tokenEntity);
            throw new InvalidRefreshTokenException("Refresh token has been revoked");
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

        Instant accessExpiresAt = Instant.now().plus(tokenProvider.accessTokenValidity());
        Instant refreshExpiresAt = Instant.now().plus(tokenProvider.refreshTokenValidity());

        RefreshTokenEntity newToken = new RefreshTokenEntity();
        newToken.setSessionProfileId(tokenEntity.getSessionProfileId());
        newToken.setTokenHash(RefreshTokenHasher.sha256(newRefreshToken));
        newToken.setCreatedAt(Instant.now());
        newToken.setExpiresAt(refreshExpiresAt);

        authenticationRepository.saveRefreshToken(newToken, tokenEntity);

        try {
            var session = sessionProfileRepository.findFirstByProfileIdOrderByIdDesc(authData.profileId);
            if (session != null) {
                session.setDateEnd(refreshExpiresAt);
                session.setStatusSession("Active");
                sessionProfileRepository.save(session);
            }
        } catch (Exception e) {
            log.warn("No se pudo actualizar el SessionProfile del perfil {}: {}", authData.profileId, e.getMessage());
        }

        return new RefreshTokenUseCase.RefreshResponse(newAccessToken, accessExpiresAt, newRefreshToken, refreshExpiresAt);
    }

    private void handleReuseDetection(RefreshTokenEntity token) {
        log.warn("Refresh token reuse detected for token {}. Revoking all tokens in session {}.", token.getId(), token.getSessionProfileId());
        authenticationRepository.revokeAllTokensInSession(token.getSessionProfileId());
    }

    public static class InvalidRefreshTokenException extends RuntimeException {
        public InvalidRefreshTokenException(String message) { super(message); }
    }
    public static class AccountInactiveException extends RuntimeException {
        public AccountInactiveException(String message) { super(message); }
    }
}
