package com.school_guardian.ms_iam.infrastructure.persistence.repository;

import com.school_guardian.ms_iam.infrastructure.persistence.entity.LoginAttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoginAttemptJpaRepository extends JpaRepository<LoginAttemptEntity, Long> {
    Optional<LoginAttemptEntity> findByEmail(String email);
}
