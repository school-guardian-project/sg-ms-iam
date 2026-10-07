package com.school_guardian.ms_iam.application.usecase;

import com.school_guardian.ms_iam.domain.exception.ProfileNotFoundException;
import com.school_guardian.ms_iam.domain.port.in.UpdateProfilePhoneUseCase;
import com.school_guardian.ms_iam.domain.port.out.ProfileRepository;
import com.school_guardian.ms_iam.shared.PhoneNormalizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UpdateProfilePhoneService implements UpdateProfilePhoneUseCase {

    private final ProfileRepository profileRepository;
    private final String defaultCountryCode;

    public UpdateProfilePhoneService(
            ProfileRepository profileRepository,
            @Value("${app.phone.default-country-code:57}") String defaultCountryCode) {
        this.profileRepository = profileRepository;
        this.defaultCountryCode = defaultCountryCode;
    }

    @Override
    public void execute(UpdatePhone command) {
        long nationalNumber = PhoneNormalizer.toNationalNumber(command.newPhone(), defaultCountryCode);

        // Phones are not unique per person (family members may share one), so there is no uniqueness check.
        if (!profileRepository.updatePhone(command.profileId(), nationalNumber)) {
            throw new ProfileNotFoundException();
        }

        log.info("Phone updated for profile {}", command.profileId());
    }
}