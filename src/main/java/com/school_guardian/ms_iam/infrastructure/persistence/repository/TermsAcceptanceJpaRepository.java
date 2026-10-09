package com.school_guardian.ms_iam.infrastructure.persistence.repository;

import com.school_guardian.ms_iam.infrastructure.persistence.entity.TermsAcceptanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TermsAcceptanceJpaRepository extends JpaRepository<TermsAcceptanceEntity, UUID> {
    List<TermsAcceptanceEntity> findByProfileIdOrderByAcceptedAtDesc(UUID profileId);
}
