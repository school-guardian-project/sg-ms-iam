package com.school_guardian.ms_iam.infrastructure.web;

import com.school_guardian.ms_iam.application.usecase.LoginService;
import com.school_guardian.ms_iam.application.usecase.RefreshTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");

    @Test
    void invalidCredentialsReturns401() {
        var response = handler.handleInvalidCredentials(
            new LoginService.InvalidCredentialsException("Invalid credentials"), request);

        assertEquals(401, response.getStatusCode().value());
        assertEquals("Invalid credentials", response.getBody().message());
        assertEquals("/api/v1/auth/login", response.getBody().path());
    }

    @Test
    void invalidRefreshTokenReturns401() {
        var response = handler.handleInvalidCredentials(
            new RefreshTokenService.InvalidRefreshTokenException("Refresh token expired"), request);

        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    void inactiveAccountReturns403() {
        var response = handler.handleAccountInactive(
            new LoginService.AccountInactiveException("Account is inactive"), request);

        assertEquals(403, response.getStatusCode().value());
    }
}
