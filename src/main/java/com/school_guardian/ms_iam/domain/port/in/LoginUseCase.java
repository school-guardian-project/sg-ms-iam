package com.school_guardian.ms_iam.domain.port.in;

import com.school_guardian.ms_iam.application.dto.LoginResponseDto;

public interface LoginUseCase {

    /** Stored in the session when the web layer could not resolve the client (column is NOT NULL). */
    String UNKNOWN_CLIENT_IP = "unknown";

    LoginResponseDto execute(Login login);

    /**
     * @param clientIp real client address, resolved by the web adapter behind the
     *                 trusted proxies; may be null outside an HTTP request
     */
    record Login(String email, String password, String clientIp) {
        public Login(String email, String password) {
            this(email, password, null);
        }
    }
}
