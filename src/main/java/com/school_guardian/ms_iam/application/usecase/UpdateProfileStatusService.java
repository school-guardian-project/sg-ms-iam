package com.school_guardian.ms_iam.application.usecase;

import com.school_guardian.ms_iam.infrastructure.persistence.entity.ProfileEntity;
import com.school_guardian.ms_iam.infrastructure.persistence.repository.ProfileJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Mirrors a person's status change onto the {@code Iam.Profile} row so login
 * (which only accepts active profiles) and driver assignments stay consistent
 * with {@code UserManagement.Person}. Idempotent: applying the same status twice
 * is a no-op, and unknown status values are ignored instead of corrupting the
 * column.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateProfileStatusService {

    private static final Set<String> ALLOWED_STATUSES = Set.of("Active", "Inactive");

    private final ProfileJpaRepository profiles;

    @Transactional
    public void execute(UUID personId, String status) {
        if (personId == null || !ALLOWED_STATUSES.contains(status)) {
            log.warn("Ignoring status change for person {}: unsupported status '{}'", personId, status);
            return;
        }

        Optional<ProfileEntity> found = profiles.findByPersonId(personId);
        if (found.isEmpty()) {
            log.warn("No profile found for person {} while applying status {}", personId, status);
            return;
        }

        ProfileEntity profile = found.get();
        if (status.equals(profile.getStatus())) {
            return;
        }

        profile.setStatus(status);
        profiles.save(profile);
    }
}
