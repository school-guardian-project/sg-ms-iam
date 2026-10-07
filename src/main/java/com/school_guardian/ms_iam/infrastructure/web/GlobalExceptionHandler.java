package com.school_guardian.ms_iam.infrastructure.web;

import com.school_guardian.ms_iam.application.dto.ErrorResponseDto;
import com.school_guardian.ms_iam.application.usecase.LoginService;
import com.school_guardian.ms_iam.application.usecase.RefreshTokenService;
import com.school_guardian.ms_iam.domain.exception.EmailAlreadyInUseException;
import com.school_guardian.ms_iam.domain.exception.ProfileNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
        LoginService.InvalidCredentialsException.class,
        RefreshTokenService.InvalidRefreshTokenException.class
    })
    public ResponseEntity<ErrorResponseDto> handleInvalidCredentials(Exception ex, HttpServletRequest request) {
        log.warn("Authentication rejected: {}", ex.getMessage());
        return response(HttpStatus.UNAUTHORIZED, ex.getMessage(), request, null);
    }

    @ExceptionHandler({
        LoginService.AccountInactiveException.class,
        RefreshTokenService.AccountInactiveException.class
    })
    public ResponseEntity<ErrorResponseDto> handleAccountInactive(Exception ex, HttpServletRequest request) {
        return response(HttpStatus.FORBIDDEN, ex.getMessage(), request, null);
    }

    @ExceptionHandler(ProfileNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleProfileNotFound(HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "Profile not found", request, null);
    }

    @ExceptionHandler(EmailAlreadyInUseException.class)
    public ResponseEntity<ErrorResponseDto> handleEmailInUse(HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "Email already in use", request, null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDto> handleBadInput(IllegalArgumentException ex, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
    }

    @ExceptionHandler(MissingRequestCookieException.class)
    public ResponseEntity<ErrorResponseDto> handleMissingCookie(HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "Missing refresh token cookie", request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
            .forEach(fe -> errors.put(fe.getField(), fe.getDefaultMessage()));
        return response(HttpStatus.BAD_REQUEST, "Validation failed", request, errors);
    }

    private ResponseEntity<ErrorResponseDto> response(
        HttpStatus status, String message, HttpServletRequest request, Map<String, String> validationErrors
    ) {
        return ResponseEntity.status(status).body(new ErrorResponseDto(
            Instant.now(), status.value(), status.getReasonPhrase(),
            message, request.getRequestURI(), validationErrors
        ));
    }
}
