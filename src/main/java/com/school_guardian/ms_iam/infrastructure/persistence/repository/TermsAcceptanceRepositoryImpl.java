package com.school_guardian.ms_iam.infrastructure.persistence.repository;

import com.school_guardian.ms_iam.domain.model.TermsAcceptance;
import com.school_guardian.ms_iam.domain.port.out.TermsAcceptanceRepository;
import com.school_guardian.ms_iam.infrastructure.persistence.entity.TermsAcceptanceEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class TermsAcceptanceRepositoryImpl implements TermsAcceptanceRepository {
    private final TermsAcceptanceJpaRepository termsAcceptanceJpaRepository;

    public TermsAcceptanceRepositoryImpl(TermsAcceptanceJpaRepository termsAcceptanceJpaRepository) {
        this.termsAcceptanceJpaRepository = termsAcceptanceJpaRepository;
    }

    @Override
    public TermsAcceptance save(TermsAcceptance acceptance) {
        if (acceptance.getId() == null) {
            acceptance.setId(UUID.randomUUID());
        }
        TermsAcceptanceEntity entity = new TermsAcceptanceEntity();
        entity.setId(acceptance.getId());
        entity.setProfileId(acceptance.getProfileId());
        entity.setStudentProfileId(acceptance.getStudentProfileId());
        entity.setTermsVersion(acceptance.getTermsVersion());
        entity.setAcceptedAt(acceptance.getAcceptedAt());
        entity.setIpAddress(acceptance.getIpAddress());
        entity.setChannel(acceptance.getChannel());

        TermsAcceptanceEntity saved = termsAcceptanceJpaRepository.save(entity);
        acceptance.setId(saved.getId());
        return acceptance;
    }

    @Override
    public List<TermsAcceptance> findByProfileId(UUID profileId) {
        return termsAcceptanceJpaRepository.findByProfileIdOrderByAcceptedAtDesc(profileId)
            .stream()
            .map(entity -> new TermsAcceptance(
                entity.getId(),
                entity.getProfileId(),
                entity.getStudentProfileId(),
                entity.getTermsVersion(),
                entity.getAcceptedAt(),
                entity.getIpAddress(),
                entity.getChannel()
            ))
            .toList();
    }
}
