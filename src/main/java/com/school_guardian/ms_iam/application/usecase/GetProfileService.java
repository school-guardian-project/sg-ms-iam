package com.school_guardian.ms_iam.application.usecase;

import com.school_guardian.ms_iam.application.dto.ProfileResponseDto;
import com.school_guardian.ms_iam.domain.port.in.AuthenticationRepository;
import com.school_guardian.ms_iam.domain.port.in.GetProfileUseCase;
import com.school_guardian.ms_iam.domain.port.out.TokenDenyList;
import com.school_guardian.ms_iam.domain.port.out.TokenProvider;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetProfileService implements GetProfileUseCase {

    private final TokenProvider tokenProvider;
    private final TokenDenyList tokenDenyList;
    private final AuthenticationRepository authenticationRepository;

    @Override
    public ProfileResponseDto execute(GetProfile command) {
        TokenProvider.TokenClaims claims;
        try {
            claims = tokenProvider.parseAccessToken(command.accessToken());
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException("Invalid access token");
        }

        if (tokenProvider.isTokenExpired(command.accessToken())) {
            throw new InvalidTokenException("Access token expired");
        }

        if (tokenDenyList.contains(claims.jti())) {
            throw new InvalidTokenException("Token revoked");
        }

        var view = authenticationRepository.findProfileViewById(claims.profileId())
            .orElseThrow(() -> new InvalidTokenException("Profile not found"));

        return new ProfileResponseDto(
            view.profileId, view.personId, view.email, view.roleId, view.roleName,
            view.campusId, view.campusName, view.schoolId, view.schoolName,
            view.name, view.lastName, view.status
        );
    }
}
