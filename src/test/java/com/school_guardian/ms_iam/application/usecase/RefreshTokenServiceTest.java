package com.school_guardian.ms_iam.application.usecase;

import com.school_guardian.ms_iam.application.dto.AuthenticationData;
import com.school_guardian.ms_iam.domain.port.in.AuthenticationRepository;
import com.school_guardian.ms_iam.domain.port.in.RefreshTokenUseCase;
import com.school_guardian.ms_iam.domain.port.out.TokenProvider;
import com.school_guardian.ms_iam.infrastructure.persistence.entity.RefreshTokenEntity;
import com.school_guardian.ms_iam.infrastructure.persistence.entity.SessionProfileEntity;
import com.school_guardian.ms_iam.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import com.school_guardian.ms_iam.infrastructure.persistence.repository.SessionProfileJpaRepository;
import com.school_guardian.ms_iam.infrastructure.security.RefreshTokenHasher;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FR-2.2 (renew the access token with a refresh token).
 *
 * <p>The refresh flow rotates the token and, because a rotated token that shows up
 * again is the signal of theft, the tests cover both the happy path and the reuse
 * path that must burn the whole session family.
 */
class RefreshTokenServiceTest {

    private final AuthenticationRepository authenticationRepository = mock(AuthenticationRepository.class);
    private final SessionProfileJpaRepository sessionProfileRepository = mock(SessionProfileJpaRepository.class);
    private final RefreshTokenJpaRepository refreshTokenRepository = mock(RefreshTokenJpaRepository.class);
    private final TokenProvider tokenProvider = mock(TokenProvider.class);
    private final RefreshTokenService service =
        new RefreshTokenService(authenticationRepository, sessionProfileRepository, refreshTokenRepository, tokenProvider);

    private final UUID profileId = UUID.randomUUID();
    private final UUID personId = UUID.randomUUID();
    private final UUID campusId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final Integer sessionId = 42;

    private TokenProvider.RefreshClaims claimsExpiringAt(Instant expiresAt) {
        return new TokenProvider.RefreshClaims(
            profileId, personId, "ana@school.edu", (byte) 2, campusId, schoolId,
            UUID.randomUUID().toString(), Instant.now().minusSeconds(60), expiresAt);
    }

    private AuthenticationData accountWithStatus(String status) {
        AuthenticationData data = new AuthenticationData();
        data.profileId = profileId;
        data.personId = personId;
        data.email = "ana@school.edu";
        data.passwordHash = "hashed";
        data.roleId = (byte) 2;
        data.roleName = "STUDENT";
        data.identificationNumber = "123456";
        data.status = status;
        data.campusId = campusId;
        data.schoolId = schoolId;
        data.permissions = List.of("student:read");
        return data;
    }

    private SessionProfileEntity session() {
        SessionProfileEntity session = new SessionProfileEntity();
        session.setId(sessionId);
        session.setProfileId(profileId);
        session.setStatusSession("Active");
        return session;
    }

    private RefreshTokenEntity storedToken(String rawToken, Instant expiresAt, Instant revokedAt) {
        RefreshTokenEntity token = new RefreshTokenEntity();
        token.setId(1L);
        token.setSessionProfileId(sessionId);
        token.setTokenHash(RefreshTokenHasher.sha256(rawToken));
        token.setCreatedAt(Instant.now().minusSeconds(60));
        token.setExpiresAt(expiresAt);
        token.setRevokedAt(revokedAt);
        return token;
    }

    private void expectAValidRefreshToken(RefreshTokenEntity stored) {
        when(tokenProvider.parseRefreshToken("valid-refresh"))
            .thenReturn(claimsExpiringAt(Instant.now().plusSeconds(3600)));
        when(refreshTokenRepository.findByTokenHashForUpdate(RefreshTokenHasher.sha256("valid-refresh")))
            .thenReturn(Optional.of(stored));
        when(sessionProfileRepository.findById(sessionId)).thenReturn(Optional.of(session()));
        when(tokenProvider.accessTokenValidity()).thenReturn(Duration.ofMinutes(15));
        when(tokenProvider.refreshTokenValidity()).thenReturn(Duration.ofDays(7));
        when(tokenProvider.generatedAccessToken(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn("new-access-token");
        when(tokenProvider.generatedRefreshToken(any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn("new-refresh-token");
        when(refreshTokenRepository.save(any(RefreshTokenEntity.class))).thenAnswer(invocation -> {
            RefreshTokenEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(2L);
            }
            return entity;
        });
    }

    @Test
    void issuesANewTokenPairAndLinksTheRotation() {
        RefreshTokenEntity stored = storedToken("valid-refresh", Instant.now().plusSeconds(3600), null);
        expectAValidRefreshToken(stored);
        when(authenticationRepository.findByProfileId(profileId)).thenReturn(Optional.of(accountWithStatus("Active")));

        RefreshTokenUseCase.RefreshResponse response =
            service.execute(new RefreshTokenUseCase.Refresh("valid-refresh"));

        assertEquals("new-access-token", response.accessToken());
        assertEquals("new-refresh-token", response.refreshToken());
        assertNotNull(stored.getRevokedAt(), "the presented token must be revoked");
        assertEquals(2L, stored.getReplacedByTokenId(), "it must point at its successor");
    }

    @Test
    void bothNewExpirationsAreInTheFutureAndRefreshOutlivesAccess() {
        RefreshTokenEntity stored = storedToken("valid-refresh", Instant.now().plusSeconds(3600), null);
        expectAValidRefreshToken(stored);
        when(authenticationRepository.findByProfileId(profileId)).thenReturn(Optional.of(accountWithStatus("Active")));

        RefreshTokenUseCase.RefreshResponse response =
            service.execute(new RefreshTokenUseCase.Refresh("valid-refresh"));

        assertTrue(response.accessTokenExpiresAt().isAfter(Instant.now()));
        assertTrue(response.refreshTokenExpiresAt().isAfter(response.accessTokenExpiresAt()));

        long accessMinutes = Duration.between(Instant.now(), response.accessTokenExpiresAt()).toMinutes();
        long refreshDays = Duration.between(Instant.now(), response.refreshTokenExpiresAt()).toDays();
        assertTrue(accessMinutes >= 14 && accessMinutes <= 15,
            "access token should live the configured 15 minutes, was " + accessMinutes + " min");
        assertTrue(refreshDays >= 6 && refreshDays <= 7,
            "refresh token should live the configured 7 days, was " + refreshDays + " d");
    }

    @Test
    void rejectsAnExpiredRefreshToken() {
        when(tokenProvider.parseRefreshToken("stale-refresh"))
            .thenReturn(claimsExpiringAt(Instant.now().minusSeconds(1)));

        assertThrows(RefreshTokenService.InvalidRefreshTokenException.class,
            () -> service.execute(new RefreshTokenUseCase.Refresh("stale-refresh")));

        verifyNoInteractions(authenticationRepository);
    }

    @Test
    void rejectsARefreshTokenThatWasAlwaysUnknown() {
        when(tokenProvider.parseRefreshToken("unknown-refresh"))
            .thenReturn(claimsExpiringAt(Instant.now().plusSeconds(3600)));
        when(refreshTokenRepository.findByTokenHashForUpdate(RefreshTokenHasher.sha256("unknown-refresh")))
            .thenReturn(Optional.empty());

        assertThrows(RefreshTokenService.InvalidRefreshTokenException.class,
            () -> service.execute(new RefreshTokenUseCase.Refresh("unknown-refresh")));

        verifyNoInteractions(authenticationRepository);
    }

    @Test
    void reusingARotatedTokenRevokesTheWholeFamily() {
        // RevokedAt != null means it was already exchanged: this is the theft signal.
        RefreshTokenEntity reused = storedToken("reused-refresh", Instant.now().plusSeconds(3600), Instant.now().minusSeconds(30));
        RefreshTokenEntity stillActive = storedToken("active-refresh", Instant.now().plusSeconds(3600), null);
        stillActive.setId(3L);

        when(tokenProvider.parseRefreshToken("reused-refresh"))
            .thenReturn(claimsExpiringAt(Instant.now().plusSeconds(3600)));
        when(refreshTokenRepository.findByTokenHashForUpdate(RefreshTokenHasher.sha256("reused-refresh")))
            .thenReturn(Optional.of(reused));
        when(refreshTokenRepository.findBySessionProfileIdAndRevokedAtIsNull(sessionId))
            .thenReturn(List.of(stillActive));
        when(sessionProfileRepository.findById(sessionId)).thenReturn(Optional.of(session()));

        assertThrows(RefreshTokenService.InvalidRefreshTokenException.class,
            () -> service.execute(new RefreshTokenUseCase.Refresh("reused-refresh")));

        assertNotNull(stillActive.getRevokedAt(), "every active token of the family must be revoked");
        verify(refreshTokenRepository).saveAll(anyList());
        verify(authenticationRepository, never()).findByProfileId(any());
    }

    @Test
    void rejectsARefreshTokenWhoseProfileNoLongerExists() {
        RefreshTokenEntity stored = storedToken("valid-refresh", Instant.now().plusSeconds(3600), null);
        expectAValidRefreshToken(stored);
        when(authenticationRepository.findByProfileId(profileId)).thenReturn(Optional.empty());

        assertThrows(RefreshTokenService.InvalidRefreshTokenException.class,
            () -> service.execute(new RefreshTokenUseCase.Refresh("valid-refresh")));
    }

    @Test
    void rejectsARefreshTokenForAnInactiveAccount() {
        RefreshTokenEntity stored = storedToken("valid-refresh", Instant.now().plusSeconds(3600), null);
        expectAValidRefreshToken(stored);
        when(authenticationRepository.findByProfileId(profileId)).thenReturn(Optional.of(accountWithStatus("Inactive")));

        assertThrows(RefreshTokenService.AccountInactiveException.class,
            () -> service.execute(new RefreshTokenUseCase.Refresh("valid-refresh")));
    }
}
