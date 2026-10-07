package com.school_guardian.ms_iam.domain.port.in;

import java.util.UUID;

public interface UpdateProfileEmailUseCase {
    void execute(UpdateEmail command);

    record UpdateEmail(UUID profileId, String newEmail) {}
}