package com.school_guardian.ms_iam.application.usecase;

import com.school_guardian.ms_iam.application.dto.AuthenticationData;
import com.school_guardian.ms_iam.application.dto.ChangePasswordRequestDto;
import com.school_guardian.ms_iam.domain.exception.InvalidPasswordException;
import com.school_guardian.ms_iam.domain.exception.ProfileNotFoundException;
import com.school_guardian.ms_iam.domain.port.in.AuthenticationRepository;
import com.school_guardian.ms_iam.domain.port.in.ChangePasswordUseCase;
import com.school_guardian.ms_iam.domain.port.out.TokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChangePasswordService implements ChangePasswordUseCase {

    private final TokenProvider tokenProvider;
    private final AuthenticationRepository authenticationRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void execute(String accessToken, ChangePasswordRequestDto request) {
        TokenProvider.TokenClaims claims = tokenProvider.parseAccessToken(accessToken);
        UUID profileId = claims.profileId();

        AuthenticationData authData = authenticationRepository.findByProfileId(profileId)
            .orElseThrow(() -> new ProfileNotFoundException("Profile not found"));

        if (!passwordEncoder.matches(request.currentPassword(), authData.passwordHash)) {
            throw new InvalidPasswordException("Current password is incorrect");
        }

        String newPasswordHash = passwordEncoder.encode(request.newPassword());
        authenticationRepository.updatePassword(profileId, newPasswordHash);
    }
}
