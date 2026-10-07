package com.school_guardian.ms_iam.domain.port.in;

import com.school_guardian.ms_iam.application.dto.ChangePasswordRequestDto;

public interface ChangePasswordUseCase {
    void execute(String accessToken, ChangePasswordRequestDto request);
}
