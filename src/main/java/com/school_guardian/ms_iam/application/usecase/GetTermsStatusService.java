package com.school_guardian.ms_iam.application.usecase;

import com.school_guardian.ms_iam.domain.port.in.GetProfileUseCase;
import com.school_guardian.ms_iam.domain.port.in.GetTermsStatusUseCase;
import com.school_guardian.ms_iam.domain.port.out.TermsAcceptanceRepository;
import com.school_guardian.ms_iam.domain.port.out.TokenDenyList;
import com.school_guardian.ms_iam.domain.port.out.TokenProvider;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetTermsStatusService implements GetTermsStatusUseCase {

    private final TokenProvider tokenProvider;
    private final TokenDenyList tokenDenyList;
    private final TermsAcceptanceRepository termsAcceptanceRepository;

    @Value("${app.terms.current-version:2026.08}")
    private String currentTermsVersion;

    @Override
    public TermsStatus execute(GetTermsStatus command) {
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

        boolean ownAccepted = termsAcceptanceRepository.findByProfileId(claims.profileId())
            .stream()
            .anyMatch(acceptance ->
                currentTermsVersion.equals(acceptance.getTermsVersion())
                    && acceptance.getStudentProfileId() == null);

        boolean authorizedByParent = termsAcceptanceRepository.findByStudentProfileId(claims.profileId())
            .stream()
            .anyMatch(acceptance -> currentTermsVersion.equals(acceptance.getTermsVersion()));

        return new TermsStatus(currentTermsVersion, ownAccepted || authorizedByParent);
    }
}
