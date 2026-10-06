package com.school_guardian.ms_iam.domain.port.in;

public interface LogoutUseCase {
    void execute(Logout logout);
     record Logout(String accessToken) {}
}
