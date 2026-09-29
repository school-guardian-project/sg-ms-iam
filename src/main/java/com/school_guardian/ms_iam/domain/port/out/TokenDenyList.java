package com.school_guardian.ms_iam.domain.port.out;

import java.time.Instant;

public interface TokenDenyList {
    void add(String jti, Instant expiresAt);
    boolean contains(String jti);
    void cleanup();
}
