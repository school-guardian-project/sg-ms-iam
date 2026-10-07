package com.school_guardian.ms_iam.application.usecase;

import com.school_guardian.ms_iam.domain.exception.ProfileNotFoundException;
import com.school_guardian.ms_iam.domain.port.in.UpdateProfilePasswordUseCase;
import com.school_guardian.ms_iam.domain.port.out.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateProfilePasswordService implements UpdateProfilePasswordUseCase {

    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void execute(UpdatePassword command) {
        String hash = passwordEncoder.encode(command.newPassword());

        if (!profileRepository.updatePasswordHash(command.profileId(), hash)) {
            throw new ProfileNotFoundException();
        }

        log.info("Password updated for profile {}", command.profileId());
    }
}