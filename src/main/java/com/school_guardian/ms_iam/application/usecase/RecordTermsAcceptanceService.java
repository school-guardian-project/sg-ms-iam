package com.school_guardian.ms_iam.application.usecase;

import com.school_guardian.ms_iam.domain.model.TermsAcceptance;
import com.school_guardian.ms_iam.domain.port.in.GetProfileUseCase;
import com.school_guardian.ms_iam.domain.port.in.RecordTermsAcceptanceUseCase;
import com.school_guardian.ms_iam.domain.port.out.TermsAcceptanceRepository;
import com.school_guardian.ms_iam.domain.port.out.TokenDenyList;
import com.school_guardian.ms_iam.domain.port.out.TokenProvider;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecordTermsAcceptanceService implements RecordTermsAcceptanceUseCase {

    private final TokenProvider tokenProvider;
    private final TokenDenyList tokenDenyList;
    private final TermsAcceptanceRepository termsAcceptanceRepository;

    @Override
    public List<UUID> execute(RecordTermsAcceptance command) {
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

        List<UUID> recorded = new ArrayList<>();
        List<UUID> subjects = command.studentProfileIds() == null || command.studentProfileIds().isEmpty()
            ? java.util.Collections.singletonList(null)
            : command.studentProfileIds();

        for (UUID studentProfileId : subjects) {
            TermsAcceptance acceptance = new TermsAcceptance();
            acceptance.setProfileId(claims.profileId());
            acceptance.setStudentProfileId(studentProfileId);
            acceptance.setTermsVersion(command.termsVersion());
            acceptance.setAcceptedAt(Instant.now());
            acceptance.setIpAddress(command.ipAddress());
            acceptance.setChannel(command.channel() == null ? "mobile" : command.channel());

            recorded.add(termsAcceptanceRepository.save(acceptance).getId());
        }

        return recorded;
    }
}
