package com.school_guardian.ms_iam.infrastructure.persistence.repository;

import com.school_guardian.ms_iam.domain.port.out.LoginAttemptRepository;
import com.school_guardian.ms_iam.infrastructure.persistence.entity.LoginAttemptEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LoginAttemptRepositoryImpl implements LoginAttemptRepository {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCKOUT_SECONDS = 30 * 60; // 30 minutos

    private final LoginAttemptJpaRepository jpaRepository;

    @Override
    public boolean isLocked(String email) {
        return jpaRepository.findByEmail(email)
            .map(e -> e.getLockedUntil() != null && e.getLockedUntil().isAfter(Instant.now()))
            .orElse(false);
    }

    @Override
    public void recordFailedAttempt(String email) {
        var now = Instant.now();
        var entity = jpaRepository.findByEmail(email).orElseGet(() -> {
            var e = new LoginAttemptEntity();
            e.setEmail(email);
            e.setFailedAttempts(0);
            e.setLockedUntil(null);
            return e;
        });

        entity.setFailedAttempts(entity.getFailedAttempts() + 1);

        if (entity.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
            entity.setLockedUntil(now.plusSeconds(LOCKOUT_SECONDS));
        }

        jpaRepository.save(entity);
    }

    @Override
    public void resetFailedAttempts(String email) {
        jpaRepository.findByEmail(email).ifPresent(entity -> {
            entity.setFailedAttempts(0);
            entity.setLockedUntil(null);
            jpaRepository.save(entity);
        });
    }

    @Override
    public Optional<Instant> getLockedUntil(String email) {
        return jpaRepository.findByEmail(email)
            .filter(e -> e.getLockedUntil() != null && e.getLockedUntil().isAfter(Instant.now()))
            .map(LoginAttemptEntity::getLockedUntil);
    }
}
