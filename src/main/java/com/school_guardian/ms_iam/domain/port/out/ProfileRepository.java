package com.school_guardian.ms_iam.domain.port.out;

import com.school_guardian.ms_iam.domain.model.Profile;

import java.util.UUID;

public interface ProfileRepository {
    boolean existsByPersonId(UUID personId);
    Profile save(Profile profile);
    void assignSchoolByPersonId(UUID personId, UUID schoolId);
}
