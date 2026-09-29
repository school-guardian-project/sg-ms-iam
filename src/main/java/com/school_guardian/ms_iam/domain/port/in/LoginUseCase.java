package com.school_guardian.ms_iam.domain.port.in;

import com.school_guardian.ms_iam.application.dto.LoginResponseDto;

import java.util.UUID;

public interface LoginUseCase {
    LoginResponseDto execute(Login login);

    record Login(String email, String password) {}
}