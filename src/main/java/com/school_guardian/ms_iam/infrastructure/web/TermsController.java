package com.school_guardian.ms_iam.infrastructure.web;

import com.school_guardian.ms_iam.application.dto.AcceptanceResponse;
import com.school_guardian.ms_iam.application.dto.AcceptTermsRequest;
import com.school_guardian.ms_iam.application.mapper.TermsAcceptanceMapper;
import com.school_guardian.ms_iam.domain.port.in.GetProfileUseCase;
import com.school_guardian.ms_iam.domain.port.in.GetTermsStatusUseCase;
import com.school_guardian.ms_iam.domain.port.in.ListTermsAcceptancesUseCase;
import com.school_guardian.ms_iam.domain.port.in.RecordTermsAcceptanceUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;

/**
 * Terms-of-service endpoints, split out of {@link AuthController}.
 *
 * <p>It keeps the {@code /api/v1/auth} base path so {@code SecurityConfig}'s
 * {@code permitAll("/api/v1/auth/**")} still covers these routes, and the auth filter
 * behaviour is unchanged. Parsing and validation live in the use cases / mappers — this
 * class only adapts HTTP.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Terms", description = "Terms of service status and acceptance")
@RequiredArgsConstructor
public class TermsController {

    private final GetTermsStatusUseCase getTermsStatusUseCase;
    private final ListTermsAcceptancesUseCase listTermsAcceptancesUseCase;
    private final RecordTermsAcceptanceUseCase recordTermsAcceptanceUseCase;

    @GetMapping("/terms/status")
    @Operation(summary = "My terms coverage status", description = "Tells whether the authenticated profile is covered by the current terms version, either by own acceptance or by a parent authorization (minors)")
    public ResponseEntity<GetTermsStatusUseCase.TermsStatus> termsStatus(
        @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        String accessToken = BearerTokenSupport.extract(authHeader);
        if (accessToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            var result = getTermsStatusUseCase.execute(new GetTermsStatusUseCase.GetTermsStatus(accessToken));
            return ResponseEntity.ok(result);
        } catch (GetProfileUseCase.InvalidTokenException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping("/terms/acceptances")
    @Operation(summary = "List my terms acceptances", description = "Returns the append-only acceptance history of the authenticated profile, latest first")
    public ResponseEntity<List<AcceptanceResponse>> acceptances(
        @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        String accessToken = BearerTokenSupport.extract(authHeader);
        if (accessToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            var result = listTermsAcceptancesUseCase.execute(new ListTermsAcceptancesUseCase.ListTermsAcceptances(accessToken));
            return ResponseEntity.ok(result.stream()
                .map(TermsAcceptanceMapper::toResponse)
                .toList());
        } catch (GetProfileUseCase.InvalidTokenException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/terms/accept")
    @Operation(summary = "Record terms acceptance", description = "Records acceptance of a terms version for the authenticated profile, optionally on behalf of student profiles (minors)")
    public ResponseEntity<List<AcceptanceResponse>> acceptTerms(
        @RequestHeader(value = "Authorization", required = false) String authHeader,
        @Valid @RequestBody AcceptTermsRequest request,
        jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        String accessToken = BearerTokenSupport.extract(authHeader);
        if (accessToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            // getRemoteAddr() is the real client: Tomcat's RemoteIpValve resolves it
            // from X-Forwarded-For sent by the trusted internal proxies (Caddy/Kong).
            var command = TermsAcceptanceMapper.toCommand(request, accessToken, httpRequest.getRemoteAddr());
            var ids = recordTermsAcceptanceUseCase.execute(command);
            var result = listTermsAcceptancesUseCase.execute(new ListTermsAcceptancesUseCase.ListTermsAcceptances(accessToken));
            var recorded = new HashSet<>(ids);
            return ResponseEntity.status(HttpStatus.CREATED).body(result.stream()
                .filter(acceptance -> recorded.contains(acceptance.getId()))
                .map(TermsAcceptanceMapper::toResponse)
                .toList());
        } catch (GetProfileUseCase.InvalidTokenException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}
