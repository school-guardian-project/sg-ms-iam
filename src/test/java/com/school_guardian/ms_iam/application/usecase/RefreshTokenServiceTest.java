package com.school_guardian.ms_iam.application.usecase;

import com.school_guardian.ms_iam.application.dto.AuthenticationData;
import com.school_guardian.ms_iam.domain.port.in.AuthenticationRepository;
import com.school_guardian.ms_iam.domain.port.in.RefreshTokenUseCase;
import com.school_guardian.ms_iam.domain.port.out.TokenProvider;
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
 * <p>The refresh flow is the only way a session survives past the access token TTL, so the
 * tests cover the happy path (a brand-new token pair is issued) and every reason the use
 * case must refuse to mint one: an expired refresh token, a profile that no longer exists
 * and an account that has been deactivated.
 */
class RefreshTokenServiceTest {

    private final AuthenticationRepository authenticationRepository = mock(AuthenticationRepository.class);
    private final TokenProvider tokenProvider = mock(TokenProvider.class);
    private final RefreshTokenService service = new RefreshTokenService(authenticationRepository, tokenProvider);

    private final UUID profileId = UUID.randomUUID();
    private final UUID personId = UUID.randomUUID();
    private final UUID campusId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();

    private TokenProvider.RefreshClaims claimsExpiringAt(Instant expiresAt) {
        return new TokenProvider.RefreshClaims(
            profileId, personId, "ana@school.edu", (byte) 2, campusId, schoolId,
            Instant.now().minusSeconds(60), expiresAt);
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

    private void expectAValidRefreshToken() {
        when(tokenProvider.parseRefreshToken("valid-refresh"))
            .thenReturn(claimsExpiringAt(Instant.now().plusSeconds(3600)));
        when(tokenProvider.accessTokenValidity()).thenReturn(Duration.ofMinutes(60));
        when(tokenProvider.refreshTokenValidity()).thenReturn(Duration.ofDays(7));
    }

    @Test
    void issuesANewTokenPairForAValidRefreshToken() {
        expectAValidRefreshToken();
        when(authenticationRepository.findByProfileId(profileId)).thenReturn(Optional.of(accountWithStatus("Active")));
        when(tokenProvider.generatedAccessToken(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn("new-access-token");
        when(tokenProvider.generatedRefreshToken(any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn("new-refresh-token");

        RefreshTokenUseCase.RefreshResponse response =
            service.execute(new RefreshTokenUseCase.Refresh("valid-refresh"));

        assertEquals("new-access-token", response.accessToken());
        assertEquals("new-refresh-token", response.refreshToken());
    }

    @Test
    void bothNewExpirationsAreInTheFutureAndRefreshOutlivesAccess() {
        expectAValidRefreshToken();
        when(authenticationRepository.findByProfileId(profileId)).thenReturn(Optional.of(accountWithStatus("Active")));
        when(tokenProvider.generatedAccessToken(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn("new-access-token");
        when(tokenProvider.generatedRefreshToken(any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn("new-refresh-token");

        RefreshTokenUseCase.RefreshResponse response =
            service.execute(new RefreshTokenUseCase.Refresh("valid-refresh"));

        assertTrue(response.accessTokenExpiresAt().isAfter(Instant.now()));
        assertTrue(response.refreshTokenExpiresAt().isAfter(response.accessTokenExpiresAt()));

        long accessMinutes = Duration.between(Instant.now(), response.accessTokenExpiresAt()).toMinutes();
        long refreshDays = Duration.between(Instant.now(), response.refreshTokenExpiresAt()).toDays();
        assertTrue(accessMinutes >= 59 && accessMinutes <= 60,
            "access token should live the configured 60 minutes, was " + accessMinutes + " min");
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
    void rejectsARefreshTokenWhoseProfileNoLongerExists() {
        expectAValidRefreshToken();
        when(authenticationRepository.findByProfileId(profileId)).thenReturn(Optional.empty());

        assertThrows(RefreshTokenService.InvalidRefreshTokenException.class,
            () -> service.execute(new RefreshTokenUseCase.Refresh("valid-refresh")));
    }

    @Test
    void rejectsARefreshTokenForAnInactiveAccount() {
        expectAValidRefreshToken();
        when(authenticationRepository.findByProfileId(profileId)).thenReturn(Optional.of(accountWithStatus("Inactive")));

        assertThrows(RefreshTokenService.AccountInactiveException.class,
            () -> service.execute(new RefreshTokenUseCase.Refresh("valid-refresh")));
    }
}
