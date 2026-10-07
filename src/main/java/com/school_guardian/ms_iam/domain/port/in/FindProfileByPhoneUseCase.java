package com.school_guardian.ms_iam.domain.port.in;

import com.school_guardian.ms_iam.domain.model.PhoneLookup;

public interface FindProfileByPhoneUseCase {

    /** @param rawPhone E.164 or national format; normalized internally. */
    PhoneLookup execute(String rawPhone);
}
