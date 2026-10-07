package com.school_guardian.ms_iam.application.usecase;

import com.school_guardian.ms_iam.domain.exception.ProfileNotFoundException;
import com.school_guardian.ms_iam.domain.port.in.MatchProfilePhoneUseCase;
import com.school_guardian.ms_iam.domain.port.out.ProfileRepository;
import com.school_guardian.ms_iam.shared.PhoneNormalizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MatchProfilePhoneService implements MatchProfilePhoneUseCase {

    private final ProfileRepository profileRepository;
    private final String defaultCountryCode;

    public MatchProfilePhoneService(
            ProfileRepository profileRepository,
            @Value("${app.phone.default-country-code:57}") String defaultCountryCode) {
        this.profileRepository = profileRepository;
        this.defaultCountryCode = defaultCountryCode;
    }

    @Override
    public boolean execute(MatchPhone command) {
        long candidate = PhoneNormalizer.toNationalNumber(command.phone(), defaultCountryCode);
        long stored = profileRepository.findPhoneByProfileId(command.profileId())
                .orElseThrow(ProfileNotFoundException::new);
        return stored == candidate;
    }
}