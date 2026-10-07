package com.school_guardian.ms_iam.infrastructure.web;

import com.school_guardian.ms_iam.application.dto.ChangePasswordRequestDto;
import com.school_guardian.ms_iam.application.dto.LoginRequestDto;
import com.school_guardian.ms_iam.application.dto.LoginResponseDto;
import com.school_guardian.ms_iam.application.dto.RefreshResponseDto;
import com.school_guardian.ms_iam.application.dto.UserProfileDto;
import com.school_guardian.ms_iam.domain.port.in.ChangePasswordUseCase;
import com.school_guardian.ms_iam.domain.port.in.GetProfileUseCase;
import com.school_guardian.ms_iam.domain.port.in.LoginUseCase;
import com.school_guardian.ms_iam.domain.port.in.LogoutUseCase;
import com.school_guardian.ms_iam.domain.port.in.RefreshTokenUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Login, token refresh, logout, profile")
@RequiredArgsConstructor
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final GetProfileUseCase getProfileUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticates user by email and returns access token in body, refresh token in HttpOnly cookie")
    public ResponseEntity<LoginResponseDto> login(
        @Valid @RequestBody LoginRequestDto request,
        HttpServletResponse response
    ) {
        var login = new LoginUseCase.Login(request.email(), request.password());
        LoginResponseDto result = loginUseCase.execute(login);

        Cookie refreshCookie = new Cookie("refresh_token", result.refreshToken());
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true);
        refreshCookie.setPath("/api/v1/auth/refresh");
        refreshCookie.setMaxAge(30 * 24 * 60 * 60);
        refreshCookie.setAttribute("SameSite", "Strict");
        response.addCookie(refreshCookie);

        return ResponseEntity.ok(new LoginResponseDto(
            result.accessToken(), result.accessTokenExpiresAt(),
            result.refreshToken(), result.refreshTokenExpiresAt(),
            result.profileId(), result.personId(), result.email(),
            result.roleId(), result.campusId()
        ));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Rotates both access and refresh tokens. Web sends the HttpOnly cookie, mobile sends Authorization: Bearer <refresh-token>")
    public ResponseEntity<RefreshResponseDto> refresh(
        @RequestHeader(value = "Authorization", required = false) String authHeader,
        @CookieValue(value = "refresh_token", required = false) String cookieToken,
        HttpServletResponse response
    ) {
        String refreshToken = cookieToken != null ? cookieToken : extractToken(authHeader);
        if (refreshToken == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }

        var refresh = new RefreshTokenUseCase.Refresh(refreshToken);
        var result = refreshTokenUseCase.execute(refresh);

        Cookie newRefreshCookie = new Cookie("refresh_token", result.refreshToken());
        newRefreshCookie.setHttpOnly(true);
        newRefreshCookie.setSecure(true);
        newRefreshCookie.setPath("/api/v1/auth/refresh");
        newRefreshCookie.setMaxAge(30 * 24 * 60 * 60);
        newRefreshCookie.setAttribute("SameSite", "Strict");
        response.addCookie(newRefreshCookie);

        return ResponseEntity.ok(new RefreshResponseDto(
            result.accessToken(), result.accessTokenExpiresAt(),
            result.refreshToken(), result.refreshTokenExpiresAt()
        ));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Revokes access token and clears refresh token cookie")
    public ResponseEntity<Void> logout(
        @RequestHeader(value = "Authorization", required = false) String authHeader,
        HttpServletResponse response
    ) {
        String accessToken = extractToken(authHeader);
        logoutUseCase.execute(new LogoutUseCase.Logout(accessToken));

        Cookie clearCookie = new Cookie("refresh_token", "");
        clearCookie.setHttpOnly(true);
        clearCookie.setSecure(true);
        clearCookie.setPath("/api/v1/auth/refresh");
        clearCookie.setMaxAge(0);
        response.addCookie(clearCookie);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/profile")
    @Operation(summary = "Get current user profile", description = "Returns profile data for the authenticated user from JWT claims")
    public ResponseEntity<UserProfileDto> getProfile(
        @RequestHeader(value = "Authorization") String authHeader
    ) {
        String accessToken = extractToken(authHeader);
        if (accessToken == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }
        
        UserProfileDto profile = getProfileUseCase.execute(accessToken);
        return ResponseEntity.ok(profile);
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change user password", description = "Changes the password for the authenticated user")
    public ResponseEntity<Void> changePassword(
        @RequestHeader(value = "Authorization") String authHeader,
        @Valid @RequestBody ChangePasswordRequestDto request
    ) {
        String accessToken = extractToken(authHeader);
        if (accessToken == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }
        
        changePasswordUseCase.execute(accessToken, request);
        return ResponseEntity.noContent().build();
    }

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}