package com.school_guardian.ms_iam.domain.port.in;

import java.util.UUID;

public interface UpdateProfilePhoneUseCase {
    void execute(UpdatePhone command);

    record UpdatePhone(UUID profileId, String newPhone) {}
}