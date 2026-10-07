package com.school_guardian.ms_iam.domain.port.out;

import com.school_guardian.ms_iam.domain.model.PhoneLookup;
import com.school_guardian.ms_iam.domain.model.Profile;
import com.school_guardian.ms_iam.domain.model.ProfileLookup;

import java.util.Optional;
import java.util.UUID;

public interface ProfileRepository {
    boolean existsByPersonId(UUID personId);
    Profile save(Profile profile);

    /** Only active profiles are returned: inactive accounts must not be able to recover a password. */
    Optional<ProfileLookup> findActiveLookupByEmail(String email);

    /** @param nationalPhone national-format digits (no country code), matching Person.Phone. */
    Optional<PhoneLookup> findActiveLookupByPhone(long nationalPhone);

    /** Returns false when no profile with that id exists. */
    boolean updatePasswordHash(UUID profileId, String passwordHash);

    /** Updates the login email stored on the profile's Person. Returns false when the profile does not exist. */
    boolean updateEmail(UUID profileId, String email);

    boolean updatePhone(UUID profileId, long phone);

    java.util.Optional<Long> findPhoneByProfileId(UUID profileId);
}