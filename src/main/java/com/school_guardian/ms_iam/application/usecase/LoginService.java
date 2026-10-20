package com.school_guardian.ms_iam.application.usecase;

import com.school_guardian.ms_iam.application.dto.AuthenticationData;
import com.school_guardian.ms_iam.application.dto.LoginResponseDto;
import com.school_guardian.ms_iam.domain.model.Profile;
import com.school_guardian.ms_iam.domain.model.Role;
import com.school_guardian.ms_iam.domain.port.in.AuthenticationRepository;
import com.school_guardian.ms_iam.domain.port.in.LoginUseCase;
import com.school_guardian.ms_iam.domain.port.out.TokenProvider;
import com.school_guardian.ms_iam.infrastructure.persistence.entity.SessionProfileEntity;
import com.school_guardian.ms_iam.infrastructure.persistence.repository.SessionProfileJpaRepository;
import com.school_guardian.ms_iam.shared.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginService implements LoginUseCase {

    private static final long ACCESS_TOKEN_EXPIRATION_MINUTES = 60L;
    private static final long REFRESH_TOKEN_EXPIRATION_DAYS = 7L;

    private final AuthenticationRepository authenticationRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final SessionProfileJpaRepository sessionProfileRepository;

    @Override
    public LoginResponseDto execute(Login login) {
        var found = authenticationRepository.findByEmail(login.email());
        if (found.isEmpty()) {
            log.info("login fallido email={} motivo=correo-no-existe", login.email());
            throw new InvalidCredentialsException("Invalid credentials");
        }
        AuthenticationData authData = found.get();

        Profile profile = toProfile(authData);

        if (!passwordEncoder.matches(login.password(), profile.getPasswordHash())) {
            log.info("login fallido email={} motivo=contrasena-invalida", login.email());
            throw new InvalidCredentialsException("Invalid credentials");
        }

        if (profile.getStatus() != Status.Active) {
            log.info("login fallido email={} motivo=cuenta-inactiva", login.email());
            throw new AccountInactiveException("Account is inactive");
        }

        log.info("login exitoso email={}", authData.email);

        // Get client IP address
        String ipAddress = "127.0.0.1";
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attributes.getRequest();
            if (request != null) {
                ipAddress = request.getRemoteAddr();
            }
        } catch (Exception e) {
            log.debug("Could not obtain client IP: {}", e.getMessage());
        }

        // Create SessionProfile record
        sessionProfileRepository.save(new SessionProfileEntity(
            1,  // ID will be auto-incremented; using 1 as placeholder
            profile.getId(),
            Instant.now(),
            Instant.now().plusSeconds(REFRESH_TOKEN_EXPIRATION_DAYS * 24 * 60 * 60),
            ipAddress,
            "Active"
        ));

        String accessToken = tokenProvider.generatedAccessToken(
            profile.getId(), profile.getPersonId(), authData.email,
            profile.getRole().getId(), authData.roleName, authData.identificationNumber,
            authData.campusId, authData.schoolId, authData.permissions,
            Map.of("type", "access")
        );

        String refreshToken = tokenProvider.generatedRefreshToken(
            profile.getId(), profile.getPersonId(), authData.email,
            profile.getRole().getId(), authData.roleName, authData.identificationNumber,
            authData.campusId, authData.schoolId
        );

        return new LoginResponseDto(
            accessToken, ACCESS_TOKEN_EXPIRATION_MINUTES,
            refreshToken, REFRESH_TOKEN_EXPIRATION_DAYS,
            profile.getId(), profile.getPersonId(), authData.email,
            profile.getRole().getId(), authData.campusId, authData.schoolId
        );
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

    public static class InvalidCredentialsException extends RuntimeException {
        public InvalidCredentialsException(String message) { super(message); }
    }
    public static class AccountInactiveException extends RuntimeException {
        public AccountInactiveException(String message) { super(message); }
    }
}
