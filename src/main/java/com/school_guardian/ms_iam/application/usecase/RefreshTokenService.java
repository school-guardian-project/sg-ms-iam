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

        if (tokenProvider.isTokenExpired(refresh.refreshToken())) {
            throw new InvalidRefreshTokenException("Refresh token expired");
        }

        // refreshClaims.profileId() es el `sub` del token (Profile.Id, no Person.Id):
        // buscar por profileId. Con findByPersonId nunca encontraba nada y el
        // refresh fallaba con "Profile not found".
        var authData = authenticationRepository.findByProfileId(refreshClaims.profileId())
            .orElseThrow(() -> new InvalidRefreshTokenException("Profile not found"));

        Profile profile = toProfile(authData);

        if (profile.getStatus() != Status.Active) {
            throw new AccountInactiveException("Account is inactive");
        }

        // schoolId se arrastra desde el refresh token en vez de volver a consultar por
        // gRPC: el refresh es la via caliente (el interceptor lo llama en cada 401) y
        // la relacion admin-colegio no cambia dentro de la vida de la sesion. Si el
        // admin cambia de colegio, re-loguea.
        UUID schoolId = refreshClaims.schoolId();

        // campusId sale de la base, no del refresh token: la sede si puede cambiar
        // durante la sesion (un estudiante se traslada de sede) y es una columna de
        // Iam.Profile, sin gRPC de por medio.
        UUID campusId = profile.getCampusId();

        String newAccessJti = UUID.randomUUID().toString();
        String newAccessToken = tokenProvider.generatedAccessToken(
            profile.getId(), profile.getPersonId(), null,
            profile.getRole().getId(), campusId, schoolId,
            Map.of("jti", newAccessJti)
        );

        String newRefreshToken = tokenProvider.generatedRefreshToken(
            profile.getId(), profile.getPersonId(), null,
            profile.getRole().getId(), campusId, schoolId
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
        profile.setCampusId(data.campusId);
        return profile;
    }

    public static class InvalidRefreshTokenException extends RuntimeException {
        public InvalidRefreshTokenException(String message) { super(message); }
    }
    public static class AccountInactiveException extends RuntimeException {
        public AccountInactiveException(String message) { super(message); }
    }
}