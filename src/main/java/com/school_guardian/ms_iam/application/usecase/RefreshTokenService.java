package com.school_guardian.ms_iam.application.usecase;

import com.school_guardian.ms_iam.application.mapper.ProfileAuthMapper;
import com.school_guardian.ms_iam.domain.model.Profile;
import com.school_guardian.ms_iam.domain.port.in.AuthenticationRepository;
import com.school_guardian.ms_iam.domain.port.in.RefreshTokenUseCase;
import com.school_guardian.ms_iam.domain.port.out.TokenProvider;
import com.school_guardian.ms_iam.infrastructure.persistence.entity.RefreshTokenEntity;
import com.school_guardian.ms_iam.infrastructure.persistence.entity.SessionProfileEntity;
import com.school_guardian.ms_iam.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import com.school_guardian.ms_iam.infrastructure.persistence.repository.SessionProfileJpaRepository;
import com.school_guardian.ms_iam.infrastructure.security.RefreshTokenHasher;
import com.school_guardian.ms_iam.shared.Status;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService implements RefreshTokenUseCase {

    private final AuthenticationRepository authenticationRepository;
    private final SessionProfileJpaRepository sessionProfileRepository;
    private final RefreshTokenJpaRepository refreshTokenRepository;
    private final TokenProvider tokenProvider;

    @Override
    // Reuse detection revokes the family and then rejects the request with a 401.
    // Without noRollbackFor the RuntimeException would undo the revocation, which
    // is exactly the write that must survive when a leaked token is replayed.
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public RefreshTokenUseCase.RefreshResponse execute(RefreshTokenUseCase.Refresh refresh) {
        var refreshClaims = parse(refresh.refreshToken());

        if (refreshClaims.expiresAt().isBefore(Instant.now())) {
            throw new InvalidRefreshTokenException("Refresh token expired");
        }

        String tokenHash = RefreshTokenHasher.sha256(refresh.refreshToken());
        RefreshTokenEntity stored = refreshTokenRepository.findByTokenHashForUpdate(tokenHash)
            .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token not recognized"));

        // A token that was already rotated (RevokedAt set) is being presented again.
        // That means it leaked, so the whole session family is burned.
        if (stored.getRevokedAt() != null) {
            revokeFamily(stored.getSessionProfileId());
            log.warn("Refresh token reuse detected for session {}", stored.getSessionProfileId());
            throw new InvalidRefreshTokenException("Refresh token reuse detected");
        }

        if (stored.getExpiresAt().isBefore(Instant.now())) {
            stored.setRevokedAt(Instant.now());
            refreshTokenRepository.save(stored);
            throw new InvalidRefreshTokenException("Refresh token expired");
        }

        var authData = authenticationRepository.findByProfileId(refreshClaims.profileId())
            .orElseThrow(() -> new InvalidRefreshTokenException("Profile not found"));

        Profile profile = ProfileAuthMapper.toProfile(authData);

        if (profile.getStatus() != Status.Active) {
            throw new AccountInactiveException("Account is inactive");
        }

        SessionProfileEntity session = sessionProfileRepository.findById(stored.getSessionProfileId())
            .orElseThrow(() -> new InvalidRefreshTokenException("Session not found"));

        if (!session.getProfileId().equals(authData.profileId)) {
            throw new InvalidRefreshTokenException("Refresh token does not belong to this session");
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

        // Insert the successor first so the predecessor can point at it.
        RefreshTokenEntity next = new RefreshTokenEntity();
        next.setSessionProfileId(session.getId());
        next.setTokenHash(RefreshTokenHasher.sha256(newRefreshToken));
        next.setCreatedAt(Instant.now());
        next.setExpiresAt(refreshExpiresAt);
        next = refreshTokenRepository.save(next);

        stored.setRevokedAt(Instant.now());
        stored.setReplacedByTokenId(next.getId());
        refreshTokenRepository.save(stored);

        session.setDateEnd(refreshExpiresAt);
        session.setStatusSession("Active");
        sessionProfileRepository.save(session);

        return new RefreshTokenUseCase.RefreshResponse(newAccessToken, accessExpiresAt, newRefreshToken, refreshExpiresAt);
    }

    /**
     * Revokes every still-active token of the session and closes the session row.
     * Called when a rotated token is presented again.
     */
    private void revokeFamily(Integer sessionProfileId) {
        Instant now = Instant.now();
        List<RefreshTokenEntity> active =
            refreshTokenRepository.findBySessionProfileIdAndRevokedAtIsNull(sessionProfileId);
        active.forEach(token -> token.setRevokedAt(now));
        refreshTokenRepository.saveAll(active);

        sessionProfileRepository.findById(sessionProfileId).ifPresent(session -> {
            session.setStatusSession("Revoked");
            session.setDateEnd(now);
            sessionProfileRepository.save(session);
        });
    }

    private TokenProvider.RefreshClaims parse(String refreshToken) {
        try {
            return tokenProvider.parseRefreshToken(refreshToken);
        } catch (JwtException | IllegalArgumentException e) {
            // Expired, malformed or signed with another key: all map to 401, never 500.
            throw new InvalidRefreshTokenException("Invalid refresh token");
        }
    }

    public static class InvalidRefreshTokenException extends RuntimeException {
        public InvalidRefreshTokenException(String message) { super(message); }
    }
    public static class AccountInactiveException extends RuntimeException {
        public AccountInactiveException(String message) { super(message); }
    }
}
