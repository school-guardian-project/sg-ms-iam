package com.school_guardian.ms_iam.application.usecase;

import com.school_guardian.ms_iam.domain.model.TermsAcceptance;
import com.school_guardian.ms_iam.domain.port.in.GetProfileUseCase;
import com.school_guardian.ms_iam.domain.port.in.ListTermsAcceptancesUseCase;
import com.school_guardian.ms_iam.domain.port.out.TermsAcceptanceRepository;
import com.school_guardian.ms_iam.domain.port.out.TokenDenyList;
import com.school_guardian.ms_iam.domain.port.out.TokenProvider;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ListTermsAcceptancesService implements ListTermsAcceptancesUseCase {

    private final TokenProvider tokenProvider;
    private final TokenDenyList tokenDenyList;
    private final TermsAcceptanceRepository termsAcceptanceRepository;

    @Override
    public List<TermsAcceptance> execute(ListTermsAcceptances command) {
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

        return termsAcceptanceRepository.findByProfileId(claims.profileId());
    }
}
