package com.school_guardian.ms_iam.application.usecase;

import com.school_guardian.ms_iam.infrastructure.persistence.repository.ProfileJpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Read-only directory lookups behind the usermanagement.v1.ProfileService
 * contract: resolve a profile id to its existence and to the name of the person
 * behind it. Both checks are scoped to active profiles/people, mirroring the
 * other {@code findActive*} queries in IAM.
 */
@Service
public class ProfileDirectoryService {

    private final ProfileJpaRepository profiles;

    public ProfileDirectoryService(ProfileJpaRepository profiles) {
        this.profiles = profiles;
    }

    @Transactional(readOnly = true)
    public boolean exists(UUID profileId) {
        return profiles.countActiveById(profileId) > 0;
    }

    @Transactional(readOnly = true)
    public String name(UUID profileId) {
        return profiles.findActivePersonName(profileId).stream().findFirst()
            .map(ProfileDirectoryService::fullName)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Active profile not found"));
    }

    private static String fullName(Object[] row) {
        String first = row[0] == null ? "" : row[0].toString();
        String last = row.length > 1 && row[1] != null ? row[1].toString() : "";
        return (first + " " + last).trim();
    }
}
