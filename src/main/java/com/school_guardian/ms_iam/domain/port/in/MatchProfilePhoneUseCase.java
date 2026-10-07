package com.school_guardian.ms_iam.domain.port.in;

import java.util.UUID;

public interface MatchProfilePhoneUseCase {
    /** True when the stored phone of the profile equals the given one (after normalization). */
    boolean execute(MatchPhone command);

    record MatchPhone(UUID profileId, String phone) {}
}