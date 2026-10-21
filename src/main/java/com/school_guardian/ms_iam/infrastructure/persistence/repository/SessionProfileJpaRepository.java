package com.school_guardian.ms_iam.infrastructure.persistence.repository;

import com.school_guardian.ms_iam.infrastructure.persistence.entity.SessionProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SessionProfileJpaRepository extends JpaRepository<SessionProfileEntity, Integer> {

    /** The session to extend is the newest row of the profile (one row per login). */
    SessionProfileEntity findFirstByProfileIdOrderByIdDesc(UUID profileId);

    void deleteByProfileId(UUID profileId);
}
