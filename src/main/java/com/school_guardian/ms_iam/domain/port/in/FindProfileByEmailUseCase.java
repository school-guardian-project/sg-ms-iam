package com.school_guardian.ms_iam.domain.port.in;

import com.school_guardian.ms_iam.domain.model.ProfileLookup;

public interface FindProfileByEmailUseCase {
    ProfileLookup execute(String email);
}