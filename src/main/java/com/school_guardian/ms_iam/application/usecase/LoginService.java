package com.school_guardian.ms_iam.application.usecase;

import com.school_guardian.ms_iam.application.dto.AuthenticationData;
import com.school_guardian.ms_iam.application.dto.LoginResponseDto;
import com.school_guardian.ms_iam.domain.model.Profile;
import com.school_guardian.ms_iam.domain.model.Role;
import com.school_guardian.ms_iam.domain.port.in.AuthenticationRepository;
import com.school_guardian.ms_iam.domain.port.in.LoginUseCase;
import com.school_guardian.ms_iam.domain.port.out.SchoolDirectory;
import com.school_guardian.ms_iam.domain.port.out.TokenProvider;
import com.school_guardian.ms_iam.shared.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginService implements LoginUseCase {

    private static final int REFRESH_TOKEN_DAYS = 30;
    private static final int ACCESS_TOKEN_MINUTES = 15;

    /** Iam.Role: 1 = administrador de colegio, 5 = superadmin. */
    private static final byte ADMIN_ROLE_ID = 1;
    private static final byte SUPERADMIN_ROLE_ID = 5;

    private final AuthenticationRepository authenticationRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final SchoolDirectory schoolDirectory;

    @Override
    public LoginResponseDto execute(Login login) {
        var authData = authenticationRepository.findByEmail(login.email())
            .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        Profile profile = toProfile(authData);

        if (!passwordEncoder.matches(login.password(), profile.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        if (profile.getStatus() != Status.Active) {
            throw new AccountInactiveException("Account is inactive");
        }

        // El colegio del admin vive en ms-school-management; se resuelve por gRPC y
        // viaja como claim schoolId. Solo aplica a admins (roleId 1 y 5); para el
        // resto de roles schoolId queda null y el claim no se emite.
        UUID schoolId = resolveSchoolId(profile);

        String accessTokenJti = UUID.randomUUID().toString();
        String accessToken = tokenProvider.generatedAccessToken(
            profile.getId(), profile.getPersonId(), authData.email,
            profile.getRole().getId(), profile.getCampusId(), schoolId,
            Map.of("jti", accessTokenJti)
        );

        String refreshToken = tokenProvider.generatedRefreshToken(
            profile.getId(), profile.getPersonId(), authData.email,
            profile.getRole().getId(), profile.getCampusId(), schoolId
        );

        Instant accessExpiresAt = Instant.now().plusSeconds(ACCESS_TOKEN_MINUTES * 60);
        Instant refreshExpiresAt = Instant.now().plusSeconds(REFRESH_TOKEN_DAYS * 24 * 60 * 60);

        return new LoginResponseDto(
            accessToken, accessExpiresAt,
            refreshToken, refreshExpiresAt,
            profile.getId(), profile.getPersonId(), authData.email,
            profile.getRole().getId(), profile.getCampusId()
        );
    }

    /** Un admin sin colegio asignado entra igual, pero con schoolId ausente. */
    private UUID resolveSchoolId(Profile profile) {
        Byte roleId = profile.getRole().getId();
        if (roleId == null || (roleId != ADMIN_ROLE_ID && roleId != SUPERADMIN_ROLE_ID)) {
            return null;
        }

        var school = schoolDirectory.findAdminSchool(profile.getId());
        return school != null ? school.id() : null;
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
        // La sede viaja en el token para que el movil sepa contra que sede
        // comparar las rutas, sin tener que pedirla en cada pantalla.
        profile.setCampusId(data.campusId);
        return profile;
    }

    public static class InvalidCredentialsException extends RuntimeException {
        public InvalidCredentialsException(String message) { super(message); }
    }
    public static class AccountInactiveException extends RuntimeException {
        public AccountInactiveException(String message) { super(message); }
    }
}