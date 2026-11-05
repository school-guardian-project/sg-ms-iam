package com.school_guardian.ms_iam.domain.port.out;

import java.time.Instant;
import java.util.Optional;

public interface LoginAttemptRepository {
    boolean isLocked(String email);
    void recordFailedAttempt(String email);
    void resetFailedAttempts(String email);
    Optional<Instant> getLockedUntil(String email);
}
