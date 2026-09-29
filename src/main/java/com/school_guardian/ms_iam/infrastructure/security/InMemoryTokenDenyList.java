package com.school_guardian.ms_iam.infrastructure.security;

import com.school_guardian.ms_iam.domain.port.out.TokenDenyList;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class InMemoryTokenDenyList implements TokenDenyList {

    private final Map<String, Instant> denylist = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor();

    public InMemoryTokenDenyList() {
        cleaner.scheduleAtFixedRate(this::cleanup, 1, 1, TimeUnit.HOURS);
    }

    @Override
    public void add(String jti, Instant expiresAt) {
        denylist.put(jti, expiresAt);
    }

    @Override
    public boolean contains(String jti) {
        Instant exp = denylist.get(jti);
        if (exp == null) return false;
        if (exp.isBefore(Instant.now())) {
            denylist.remove(jti);
            return false;
        }
        return true;
    }

    @Override
    public void cleanup() {
        Instant now = Instant.now();
        denylist.entrySet().removeIf(e -> e.getValue().isBefore(now));
    }
}