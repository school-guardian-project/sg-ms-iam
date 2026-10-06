package com.school_guardian.ms_iam.infrastructure.web;

import com.school_guardian.ms_iam.application.dto.ErrorResponseDto;
import com.school_guardian.ms_iam.application.usecase.LoginService;
import com.school_guardian.ms_iam.application.usecase.RefreshTokenService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(LoginService.InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponseDto> invalidCredentials(
        LoginService.InvalidCredentialsException ex, HttpServletRequest request
    ) {
        return buildUnauthorized("INVALID_CREDENTIALS", ex.getMessage(), request);
    }

    @ExceptionHandler({
        LoginService.AccountInactiveException.class,
        RefreshTokenService.AccountInactiveException.class,
        RefreshTokenService.InvalidRefreshTokenException.class,
        JwtException.class
    })
    public ResponseEntity<ErrorResponseDto> unauthorized(
        RuntimeException ex, HttpServletRequest request
    ) {
        return buildUnauthorized("UNAUTHORIZED", ex.getMessage(), request);
    }

    private ResponseEntity<ErrorResponseDto> buildUnauthorized(
        String error, String message, HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            new ErrorResponseDto(
                Instant.now(),
                HttpStatus.UNAUTHORIZED.value(),
                error,
                message,
                request.getRequestURI(),
                null
            )
        );
    }
}
