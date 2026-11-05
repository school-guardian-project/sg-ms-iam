package com.school_guardian.ms_iam.application.usecase;

import com.school_guardian.ms_iam.domain.port.in.LoginAttemptUseCase;
import com.school_guardian.ms_iam.domain.port.out.LoginAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptService implements LoginAttemptUseCase {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(30);

    private final LoginAttemptRepository loginAttemptRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean isLocked(String email) {
        return loginAttemptRepository.isLocked(email);
    }

    @Override
    @Transactional
    public void recordFailedAttempt(String email) {
        loginAttemptRepository.recordFailedAttempt(email);
        if (loginAttemptRepository.isLocked(email)) {
            log.warn("Cuenta bloqueada tras intentos fallidos email={}", email);
        }
    }

    @Override
    @Transactional
    public void resetFailedAttempts(String email) {
        loginAttemptRepository.resetFailedAttempts(email);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Instant> getLockedUntil(String email) {
        return loginAttemptRepository.getLockedUntil(email);
    }

    public int getMaxFailedAttempts() {
        return MAX_FAILED_ATTEMPTS;
    }

    public Duration getLockoutDuration() {
        return LOCKOUT_DURATION;
    }
}
