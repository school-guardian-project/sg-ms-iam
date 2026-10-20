package com.school_guardian.ms_iam.infrastructure.persistence.repository;
import java.util.UUID;
import com.school_guardian.ms_iam.infrastructure.persistence.entity.SessionProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SessionProfileJpaRepository extends JpaRepository<SessionProfileEntity, Integer> {

    SessionProfileEntity findByProfileId(UUID profileId);

    void deleteByProfileId(UUID profileId);
}
