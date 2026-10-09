package com.school_guardian.ms_iam.infrastructure.web;

import com.school_guardian.ms_iam.application.dto.LoginRequestDto;
import com.school_guardian.ms_iam.application.dto.LoginResponseDto;
import com.school_guardian.ms_iam.application.dto.ProfileResponseDto;
import com.school_guardian.ms_iam.application.dto.RefreshResponseDto;
import com.school_guardian.ms_iam.application.dto.TermsAcceptanceDtos;
import com.school_guardian.ms_iam.domain.port.in.GetProfileUseCase;
import com.school_guardian.ms_iam.domain.port.in.ListTermsAcceptancesUseCase;
import com.school_guardian.ms_iam.domain.port.in.LoginUseCase;
import com.school_guardian.ms_iam.domain.port.in.LogoutUseCase;
import com.school_guardian.ms_iam.domain.port.in.RecordTermsAcceptanceUseCase;
import com.school_guardian.ms_iam.domain.port.in.RefreshTokenUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Login, token refresh, logout")
@RequiredArgsConstructor
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final GetProfileUseCase getProfileUseCase;
    private final RecordTermsAcceptanceUseCase recordTermsAcceptanceUseCase;
    private final ListTermsAcceptancesUseCase listTermsAcceptancesUseCase;

    @GetMapping("/profile")
    @Operation(summary = "Current user profile", description = "Returns the profile of the authenticated user, including role and tenant (campus/school)")
    public ResponseEntity<ProfileResponseDto> profile(
        @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        String accessToken = extractToken(authHeader);
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
            result.roleId(), result.campusId(), result.schoolId()
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

    @GetMapping("/terms/acceptances")
    @Operation(summary = "List my terms acceptances", description = "Returns the append-only acceptance history of the authenticated profile, latest first")
    public ResponseEntity<List<TermsAcceptanceDtos.AcceptanceResponse>> acceptances(
        @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        String accessToken = extractToken(authHeader);
        if (accessToken == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }
        try {
            var result = listTermsAcceptancesUseCase.execute(new ListTermsAcceptancesUseCase.ListTermsAcceptances(accessToken));
            return ResponseEntity.ok(result.stream()
                .map(acceptance -> new TermsAcceptanceDtos.AcceptanceResponse(
                    acceptance.getId(), acceptance.getProfileId(), acceptance.getStudentProfileId(),
                    acceptance.getTermsVersion(), acceptance.getAcceptedAt(), acceptance.getChannel()))
                .toList());
        } catch (GetProfileUseCase.InvalidTokenException e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/terms/accept")
    @Operation(summary = "Record terms acceptance", description = "Records acceptance of a terms version for the authenticated profile, optionally on behalf of student profiles (minors)")
    public ResponseEntity<List<TermsAcceptanceDtos.AcceptanceResponse>> acceptTerms(
        @RequestHeader(value = "Authorization", required = false) String authHeader,
        @Valid @RequestBody TermsAcceptanceDtos.AcceptTermsRequest request,
        jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        String accessToken = extractToken(authHeader);
        if (accessToken == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }
        try {
            var command = new RecordTermsAcceptanceUseCase.RecordTermsAcceptance(
                accessToken, request.termsVersion(), request.studentProfileIds(),
                httpRequest.getRemoteAddr(), "mobile");
            var ids = recordTermsAcceptanceUseCase.execute(command);
            var result = listTermsAcceptancesUseCase.execute(new ListTermsAcceptancesUseCase.ListTermsAcceptances(accessToken));
            var recorded = new java.util.HashSet<>(ids);
            return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(result.stream()
                .filter(acceptance -> recorded.contains(acceptance.getId()))
                .map(acceptance -> new TermsAcceptanceDtos.AcceptanceResponse(
                    acceptance.getId(), acceptance.getProfileId(), acceptance.getStudentProfileId(),
                    acceptance.getTermsVersion(), acceptance.getAcceptedAt(), acceptance.getChannel()))
                .toList());
        } catch (GetProfileUseCase.InvalidTokenException e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }
    }

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}