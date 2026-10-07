package com.school_guardian.ms_iam.application.usecase;

import com.school_guardian.ms_iam.domain.exception.ProfileNotFoundException;
import com.school_guardian.ms_iam.domain.model.ProfileLookup;
import com.school_guardian.ms_iam.domain.port.in.FindProfileByEmailUseCase;
import com.school_guardian.ms_iam.domain.port.out.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class FindProfileByEmailService implements FindProfileByEmailUseCase {

    private final ProfileRepository profileRepository;

    @Override
    public ProfileLookup execute(String email) {
        return profileRepository.findActiveLookupByEmail(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(ProfileNotFoundException::new);
    }
}