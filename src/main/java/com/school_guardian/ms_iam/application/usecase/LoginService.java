package com.school_guardian.ms_iam.application.usecase;

import com.school_guardian.ms_iam.application.dto.AuthenticationData;
import com.school_guardian.ms_iam.application.dto.LoginResponseDto;
import com.school_guardian.ms_iam.application.mapper.ProfileAuthMapper;
import com.school_guardian.ms_iam.domain.model.Profile;
import com.school_guardian.ms_iam.domain.port.in.AuthenticationRepository;
import com.school_guardian.ms_iam.domain.port.in.LoginUseCase;
import com.school_guardian.ms_iam.domain.port.out.TokenProvider;
import com.school_guardian.ms_iam.infrastructure.persistence.entity.SessionProfileEntity;
import com.school_guardian.ms_iam.infrastructure.persistence.repository.SessionProfileJpaRepository;
import com.school_guardian.ms_iam.shared.Status;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginService implements LoginUseCase {

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

        Profile profile = ProfileAuthMapper.toProfile(authData);

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

        // The expiries reported to the caller must match the ones embedded in the JWTs,
        // so both are taken from the same source (TokenProvider <- app.jwt.*).
        Instant accessExpiresAt = Instant.now().plus(tokenProvider.accessTokenValidity());
        Instant refreshExpiresAt = Instant.now().plus(tokenProvider.refreshTokenValidity());

        // SessionProfile is history: a row is appended per login and never reuses Id.
        // Id is IDENTITY in the database (Iam.SessionProfile.Id INT IDENTITY), so it is
        // left unset here and filled by the insert.
        SessionProfileEntity session = new SessionProfileEntity();
        session.setProfileId(profile.getId());
        session.setDateStart(Instant.now());
        session.setDateEnd(refreshExpiresAt);
        session.setIpAddress(ipAddress);
        session.setStatusSession("Active");
        sessionProfileRepository.save(session);

        return new LoginResponseDto(
            accessToken, accessExpiresAt,
            refreshToken, refreshExpiresAt,
            profile.getId(), profile.getPersonId(), authData.email,
            profile.getRole().getId(), authData.campusId, authData.schoolId
        );
    }

    public static class InvalidCredentialsException extends RuntimeException {
        public InvalidCredentialsException(String message) { super(message); }
    }
    public static class AccountInactiveException extends RuntimeException {
        public AccountInactiveException(String message) { super(message); }
    }
}
