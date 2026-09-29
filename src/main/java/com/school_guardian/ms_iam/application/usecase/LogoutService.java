package com.school_guardian.ms_iam.application.usecase;

import com.school_guardian.ms_iam.domain.port.in.LogoutUseCase;
import com.school_guardian.ms_iam.domain.port.out.TokenDenyList;
import com.school_guardian.ms_iam.domain.port.out.TokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogoutService implements LogoutUseCase {
    private final TokenProvider tokenProvider;
    private final TokenDenyList tokenDenyList;

    @Override
    public void execute(Logout logout) {
        if (logout.accessToken() != null) {
            try {
                String jti = tokenProvider.extractJti(logout.accessToken()).toString();
                Instant exp = tokenProvider.extractExpiration(logout.accessToken());
                tokenDenyList.add(jti, exp);
            } catch (Exception ignored) {}
        }
    }
}
