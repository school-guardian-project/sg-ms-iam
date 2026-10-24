package com.school_guardian.ms_iam.infrastructure.persistence.repository;

import com.school_guardian.ms_iam.infrastructure.persistence.entity.RefreshTokenEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenEntity, Long> {

    /**
     * Serialises concurrent refreshes of the same token. Without the lock two
     * requests could both read the token as "active" and each mint a new pair,
     * which would defeat reuse detection.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM RefreshTokenEntity t WHERE t.tokenHash = :tokenHash")
    Optional<RefreshTokenEntity> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    List<RefreshTokenEntity> findBySessionProfileIdAndRevokedAtIsNull(Integer sessionProfileId);
}
