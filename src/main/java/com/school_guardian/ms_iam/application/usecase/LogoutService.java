package com.school_guardian.ms_iam.application.usecase;

import com.school_guardian.ms_iam.domain.port.in.LogoutUseCase;
import com.school_guardian.ms_iam.domain.port.out.TokenDenyList;
import com.school_guardian.ms_iam.domain.port.out.TokenProvider;
import com.school_guardian.ms_iam.infrastructure.persistence.entity.RefreshTokenEntity;
import com.school_guardian.ms_iam.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import com.school_guardian.ms_iam.infrastructure.persistence.repository.SessionProfileJpaRepository;
import com.school_guardian.ms_iam.infrastructure.security.RefreshTokenHasher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogoutService implements LogoutUseCase {
    private final TokenProvider tokenProvider;
    private final TokenDenyList tokenDenyList;
    private final RefreshTokenJpaRepository refreshTokenRepository;
    private final SessionProfileJpaRepository sessionProfileRepository;

    @Override
    @Transactional
    public void execute(Logout logout) {
        if (logout.accessToken() != null) {
            try {
                String jti = tokenProvider.extractJti(logout.accessToken()).toString();
                Instant exp = tokenProvider.extractExpiration(logout.accessToken());
                tokenDenyList.add(jti, exp);
            } catch (Exception ignored) {}
        }

        if (logout.refreshToken() != null) {
            revokeFamily(logout.refreshToken());
        }
    }

    /**
     * Closing a session invalidates its refresh tokens so they can no longer mint
     * access tokens. Access tokens already issued still live until their 15-minute
     * expiry; that is the documented trade-off of decision 5.
     */
    private void revokeFamily(String refreshToken) {
        String tokenHash = RefreshTokenHasher.sha256(refreshToken);
        refreshTokenRepository.findByTokenHashForUpdate(tokenHash).ifPresent(token -> {
            Instant now = Instant.now();
            List<RefreshTokenEntity> active =
                refreshTokenRepository.findBySessionProfileIdAndRevokedAtIsNull(token.getSessionProfileId());
            active.forEach(entity -> entity.setRevokedAt(now));
            refreshTokenRepository.saveAll(active);

            sessionProfileRepository.findById(token.getSessionProfileId()).ifPresent(session -> {
                session.setStatusSession("Closed");
                session.setDateEnd(now);
                sessionProfileRepository.save(session);
            });
        });
    }
}
