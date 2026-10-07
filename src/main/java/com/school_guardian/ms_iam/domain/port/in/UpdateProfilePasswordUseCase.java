package com.school_guardian.ms_iam.domain.port.in;

import java.util.UUID;

public interface UpdateProfilePasswordUseCase {
    void execute(UpdatePassword command);

    record UpdatePassword(UUID profileId, String newPassword) {}
}