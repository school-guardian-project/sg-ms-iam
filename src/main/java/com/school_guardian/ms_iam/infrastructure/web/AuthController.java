package com.school_guardian.ms_iam.infrastructure.web;

import com.school_guardian.ms_iam.application.dto.LoginRequestDto;
import com.school_guardian.ms_iam.application.dto.LoginResponseDto;
import com.school_guardian.ms_iam.application.dto.ProfileResponseDto;
import com.school_guardian.ms_iam.application.dto.RefreshResponseDto;
import com.school_guardian.ms_iam.domain.port.in.GetProfileUseCase;
import com.school_guardian.ms_iam.domain.port.in.LoginUseCase;
import com.school_guardian.ms_iam.domain.port.in.LogoutUseCase;
import com.school_guardian.ms_iam.domain.port.in.RefreshTokenUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Credential endpoints (login / refresh / logout / profile). The terms-of-service
 * endpoints live in {@link TermsController}; cookie handling lives in
 * {@link RefreshCookieSupport}; header parsing in {@link BearerTokenSupport}.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Login, token refresh, logout")
@RequiredArgsConstructor
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final GetProfileUseCase getProfileUseCase;

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Simple health check endpoint for mobile app server availability verification")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "ms-iam"));
    }

    @GetMapping("/profile")
    @Operation(summary = "Current user profile", description = "Returns the profile of the authenticated user, including role and tenant (campus/school)")
    public ResponseEntity<ProfileResponseDto> profile(
        @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        String accessToken = BearerTokenSupport.extract(authHeader);
        if (accessToken == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }
        try {
            var result = getProfileUseCase.execute(new GetProfileUseCase.GetProfile(accessToken));
            return ResponseEntity.ok(result);
        } catch (GetProfileUseCase.InvalidTokenException e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticates user by email and returns access token in body, refresh token in HttpOnly cookie")
    public ResponseEntity<LoginResponseDto> login(
        @Valid @RequestBody LoginRequestDto request,
        jakarta.servlet.http.HttpServletRequest httpRequest,
        HttpServletResponse response
    ) {
        // getRemoteAddr() is the real client: Tomcat's RemoteIpValve resolves it
        // from X-Forwarded-For sent by the trusted internal proxies (Caddy/Kong).
        var login = new LoginUseCase.Login(request.email(), request.password(), httpRequest.getRemoteAddr());
        LoginResponseDto result = loginUseCase.execute(login);

        RefreshCookieSupport.attachRefreshCookie(response, result.refreshToken(), result.refreshTokenExpiresAt());

        return ResponseEntity.ok(result);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Rotates both access and refresh tokens. Web sends the HttpOnly cookie, mobile sends Authorization: Bearer <refresh-token>")
    public ResponseEntity<RefreshResponseDto> refresh(
        @RequestHeader(value = "Authorization", required = false) String authHeader,
        @CookieValue(value = "refresh_token", required = false) String cookieToken,
        HttpServletResponse response
    ) {
        String refreshToken = cookieToken != null ? cookieToken : BearerTokenSupport.extract(authHeader);
        if (refreshToken == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }

        var refresh = new RefreshTokenUseCase.Refresh(refreshToken);
        var result = refreshTokenUseCase.execute(refresh);

        RefreshCookieSupport.attachRefreshCookie(response, result.refreshToken(), result.refreshTokenExpiresAt());

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
        String accessToken = BearerTokenSupport.extract(authHeader);
        logoutUseCase.execute(new LogoutUseCase.Logout(accessToken));

        RefreshCookieSupport.clearRefreshCookie(response);

        return ResponseEntity.noContent().build();
    }
}
