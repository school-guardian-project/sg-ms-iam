package com.school_guardian.ms_iam.application.usecase;

import com.school_guardian.ms_iam.application.dto.AuthenticationData;
import com.school_guardian.ms_iam.application.dto.LoginResponseDto;
import com.school_guardian.ms_iam.domain.model.Profile;
import com.school_guardian.ms_iam.domain.model.Role;
import com.school_guardian.ms_iam.domain.port.in.AuthenticationRepository;
import com.school_guardian.ms_iam.domain.port.in.LoginUseCase;
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

    private final AuthenticationRepository authenticationRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;

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

        String accessTokenJti = UUID.randomUUID().toString();
        String accessToken = tokenProvider.generatedAccessToken(
            profile.getId(), profile.getPersonId(), authData.email,
            profile.getRole().getId(), authData.campusId, authData.schoolId,
            Map.of("jti", accessTokenJti)
        );

        String refreshToken = tokenProvider.generatedRefreshToken(
            profile.getId(), profile.getPersonId(), authData.email,
            profile.getRole().getId(), authData.campusId, authData.schoolId
        );

        Instant accessExpiresAt = Instant.now().plusSeconds(ACCESS_TOKEN_MINUTES * 60);
        Instant refreshExpiresAt = Instant.now().plusSeconds(REFRESH_TOKEN_DAYS * 24 * 60 * 60);

        return new LoginResponseDto(
            accessToken, accessExpiresAt,
            refreshToken, refreshExpiresAt,
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