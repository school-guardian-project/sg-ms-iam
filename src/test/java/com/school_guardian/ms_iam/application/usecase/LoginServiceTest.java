package com.school_guardian.ms_iam.application.usecase;

import com.school_guardian.ms_iam.application.dto.AuthenticationData;
import com.school_guardian.ms_iam.application.dto.LoginResponseDto;
import com.school_guardian.ms_iam.domain.port.in.AuthenticationRepository;
import com.school_guardian.ms_iam.domain.port.in.LoginUseCase;
import com.school_guardian.ms_iam.domain.port.out.TokenProvider;
import com.school_guardian.ms_iam.infrastructure.persistence.entity.RefreshTokenEntity;
import com.school_guardian.ms_iam.infrastructure.persistence.entity.SessionProfileEntity;
import com.school_guardian.ms_iam.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import com.school_guardian.ms_iam.infrastructure.persistence.repository.SessionProfileJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FR-2.1 (authenticate and get a JWT).
 *
 * <p>LoginService is the entry point for authentication, so these tests pin down the
 * observable contract of the use case: the identity detail it returns on success and the
 * three distinct failure modes (unknown email, wrong password, inactive account) that the
 * controller maps to HTTP responses.
 */
class LoginServiceTest {

    private final AuthenticationRepository authenticationRepository = mock(AuthenticationRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final TokenProvider tokenProvider = mock(TokenProvider.class);
    private final SessionProfileJpaRepository sessionProfileRepository = mock(SessionProfileJpaRepository.class);
    private final RefreshTokenJpaRepository refreshTokenRepository = mock(RefreshTokenJpaRepository.class);
    private final LoginService service =
        new LoginService(authenticationRepository, passwordEncoder, tokenProvider,
            sessionProfileRepository, refreshTokenRepository);

    private final UUID profileId = UUID.randomUUID();
    private final UUID personId = UUID.randomUUID();
    private final UUID campusId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();

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

    private void expectValidCredentialsForActiveAccount() {
        when(authenticationRepository.findByEmail("ana@school.edu")).thenReturn(Optional.of(accountWithStatus("Active")));
        when(passwordEncoder.matches("secret", "hashed")).thenReturn(true);
        when(tokenProvider.accessTokenValidity()).thenReturn(Duration.ofMinutes(15));
        when(tokenProvider.refreshTokenValidity()).thenReturn(Duration.ofDays(7));
        when(sessionProfileRepository.save(any(SessionProfileEntity.class))).thenAnswer(invocation -> {
            SessionProfileEntity session = invocation.getArgument(0);
            session.setId(7);
            return session;
        });
        when(tokenProvider.generatedAccessToken(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn("access-token");
        when(tokenProvider.generatedRefreshToken(any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn("refresh-token");
    }

    @Test
    void returnsBothTokensAndTheIdentityForValidCredentials() {
        expectValidCredentialsForActiveAccount();

        LoginResponseDto response = service.execute(new LoginUseCase.Login("ana@school.edu", "secret"));

        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
        assertEquals(profileId, response.profileId());
        assertEquals(personId, response.personId());
        assertEquals("ana@school.edu", response.email());
        assertEquals((byte) 2, response.roleId());
        assertEquals(campusId, response.campusId());
        assertEquals(schoolId, response.schoolId());

        verify(refreshTokenRepository).save(any(RefreshTokenEntity.class));
    }

    @Test
    void bothExpirationsAreInTheFutureAndRefreshOutlivesAccess() {
        expectValidCredentialsForActiveAccount();

        LoginResponseDto response = service.execute(new LoginUseCase.Login("ana@school.edu", "secret"));

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
    void rejectsAnUnknownEmail() {
        when(authenticationRepository.findByEmail("ghost@school.edu")).thenReturn(Optional.empty());

        assertThrows(LoginService.InvalidCredentialsException.class,
            () -> service.execute(new LoginUseCase.Login("ghost@school.edu", "secret")));
    }

    @Test
    void rejectsAWrongPassword() {
        when(authenticationRepository.findByEmail("ana@school.edu")).thenReturn(Optional.of(accountWithStatus("Active")));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThrows(LoginService.InvalidCredentialsException.class,
            () -> service.execute(new LoginUseCase.Login("ana@school.edu", "wrong")));
    }

    @Test
    void rejectsAnInactiveAccount() {
        when(authenticationRepository.findByEmail("ana@school.edu")).thenReturn(Optional.of(accountWithStatus("Inactive")));
        when(passwordEncoder.matches("secret", "hashed")).thenReturn(true);

        assertThrows(LoginService.AccountInactiveException.class,
            () -> service.execute(new LoginUseCase.Login("ana@school.edu", "secret")));
    }

    @Test
    void aRejectedLoginNeverIssuesAToken() {
        when(authenticationRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThrows(LoginService.InvalidCredentialsException.class,
            () -> service.execute(new LoginUseCase.Login("ghost@school.edu", "secret")));

        verifyNoInteractions(tokenProvider);
    }
}
