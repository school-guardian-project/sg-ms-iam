package com.school_guardian.ms_iam.application.usecase;

import com.school_guardian.ms_iam.application.dto.UserProfileDto;
import com.school_guardian.ms_iam.domain.model.Role;
import com.school_guardian.ms_iam.domain.port.in.GetProfileUseCase;
import com.school_guardian.ms_iam.domain.port.out.RoleRepository;
import com.school_guardian.ms_iam.domain.port.out.TokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetProfileService implements GetProfileUseCase {

    private final TokenProvider tokenProvider;
    private final RoleRepository roleRepository;

    @Override
    public UserProfileDto execute(String accessToken) {
        TokenProvider.TokenClaims claims = tokenProvider.parseAccessToken(accessToken);
        
        Role role = roleRepository.findById(claims.roleId());
        String roleName = role != null ? role.getName() : null;
        
        return new UserProfileDto(
            claims.profileId(),
            claims.personId(),
            claims.email(),
            claims.roleId(),
            roleName,
            claims.campusId(),
            // Se lee del token, no de la base: schoolId ya se resolvio por gRPC en
            // el login y asi /profile no agrega otra ida a ms-school-management en
            // la pantalla que el frontend usa para saber con que colegio opera.
            claims.schoolId()
        );
    }
}
