package com.school_guardian.ms_iam.application.usecase;

import com.school_guardian.ms_iam.domain.exception.ProfileNotFoundException;
import com.school_guardian.ms_iam.domain.model.PhoneLookup;
import com.school_guardian.ms_iam.domain.port.in.FindProfileByPhoneUseCase;
import com.school_guardian.ms_iam.domain.port.out.ProfileRepository;
import com.school_guardian.ms_iam.shared.PhoneNormalizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FindProfileByPhoneService implements FindProfileByPhoneUseCase {

    private final ProfileRepository profileRepository;
    private final String defaultCountryCode;

    public FindProfileByPhoneService(
            ProfileRepository profileRepository,
            @Value("${app.phone.default-country-code:57}") String defaultCountryCode) {
        this.profileRepository = profileRepository;
        this.defaultCountryCode = defaultCountryCode;
    }

    @Override
    public PhoneLookup execute(String rawPhone) {
        long nationalNumber = PhoneNormalizer.toNationalNumber(rawPhone, defaultCountryCode);
        return profileRepository.findActiveLookupByPhone(nationalNumber)
                .orElseThrow(ProfileNotFoundException::new);
    }
}
