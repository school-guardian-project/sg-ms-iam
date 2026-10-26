package com.school_guardian.ms_iam.application.usecase;

import com.school_guardian.ms_iam.domain.port.in.GetPendingAcceptancesUseCase;
import com.school_guardian.ms_iam.domain.port.in.GetProfileUseCase;
import com.school_guardian.ms_iam.domain.port.out.TermsAcceptanceRepository;
import com.school_guardian.ms_iam.domain.port.out.TokenDenyList;
import com.school_guardian.ms_iam.domain.port.out.TokenProvider;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetPendingAcceptancesService implements GetPendingAcceptancesUseCase {

    private final TokenProvider tokenProvider;
    private final TokenDenyList tokenDenyList;
    private final TermsAcceptanceRepository termsAcceptanceRepository;

    @Value("${app.terms.current-version:2026.08}")
    private String currentTermsVersion;

    @Override
    public PendingAcceptances execute(GetPendingAcceptances command) {
        TokenProvider.TokenClaims claims;
        try {
            claims = tokenProvider.parseAccessToken(command.accessToken());
        } catch (JwtException | IllegalArgumentException e) {
            throw new GetProfileUseCase.InvalidTokenException("Invalid access token");
        }

        if (tokenProvider.isTokenExpired(command.accessToken())) {
            throw new GetProfileUseCase.InvalidTokenException("Access token expired");
        }

        if (tokenDenyList.contains(claims.jti())) {
            throw new GetProfileUseCase.InvalidTokenException("Token revoked");
        }

        UUID profileId = claims.profileId();
        Integer roleId = claims.roleId() != null ? claims.roleId().intValue() : null;

        // Check own acceptance for current version
        var ownAcceptances = termsAcceptanceRepository.findByProfileId(profileId)
            .stream()
            .filter(a -> currentTermsVersion.equals(a.getTermsVersion()) && a.getStudentProfileId() == null)
            .toList();
        boolean ownMissing = ownAcceptances.isEmpty();

        // For minors: if user is a parent (roleId = 4), check linked students
        List<MinorInfo> minors = new ArrayList<>();
        if (roleId != null && roleId == 4) {
            // Get acceptances for minors already covered
            Set<UUID> coveredMinorIds = termsAcceptanceRepository.findByProfileId(profileId)
                .stream()
                .filter(a -> currentTermsVersion.equals(a.getTermsVersion()) && a.getStudentProfileId() != null)
                .map(a -> a.getStudentProfileId())
                .collect(Collectors.toSet());

            // TODO: Call user-management service via gRPC to get linked students
            // For now, return empty list - will be implemented when gRPC client is available
            log.debug("User {} is a parent, linked students check not yet implemented", profileId);
        }

        return new PendingAcceptances(currentTermsVersion, ownMissing, minors);
    }
}