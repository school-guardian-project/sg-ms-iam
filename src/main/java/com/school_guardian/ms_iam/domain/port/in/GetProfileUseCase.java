package com.school_guardian.ms_iam.domain.port.in;

import com.school_guardian.ms_iam.application.dto.UserProfileDto;

public interface GetProfileUseCase {
    UserProfileDto execute(String accessToken);
}
