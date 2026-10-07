package com.school_guardian.ms_iam.application.usecase;

import com.school_guardian.ms_iam.domain.exception.EmailAlreadyInUseException;
import com.school_guardian.ms_iam.domain.exception.ProfileNotFoundException;
import com.school_guardian.ms_iam.domain.port.in.UpdateProfileEmailUseCase;
import com.school_guardian.ms_iam.domain.port.out.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateProfileEmailService implements UpdateProfileEmailUseCase {

    private final ProfileRepository profileRepository;

    @Override
    public void execute(UpdateEmail command) {
        String email = command.newEmail().trim().toLowerCase(Locale.ROOT);

        // Person.Email is not unique in the database, so login-email uniqueness is enforced here.
        var existing = profileRepository.findActiveLookupByEmail(email);
        if (existing.isPresent() && !existing.get().profileId().equals(command.profileId())) {
            throw new EmailAlreadyInUseException();
        }

        if (!profileRepository.updateEmail(command.profileId(), email)) {
            throw new ProfileNotFoundException();
        }

        log.info("Email updated for profile {}", command.profileId());
    }
}