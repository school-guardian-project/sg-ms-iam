package com.school_guardian.ms_iam.domain.port.in;

import com.school_guardian.ms_iam.application.dto.ProfileResponseDto;

public interface GetProfileUseCase {
    ProfileResponseDto execute(GetProfile command);

    record GetProfile(String accessToken) {}

    class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(String message) { super(message); }
    }
}
