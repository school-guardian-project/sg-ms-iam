package com.school_guardian.ms_iam.infrastructure.persistence.repository;

import com.school_guardian.ms_iam.infrastructure.persistence.entity.ProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProfileJpaRepository extends JpaRepository<ProfileEntity, UUID> {
    boolean existsByPersonId(UUID personId);

    @Query(value = """
        SELECT p.Id, p.PersonId, p.PasswordHash, p.RoleId, p.Status, per.Email, p.CampuseId
        FROM Iam.Profile p
        INNER JOIN UserManagement.Person per ON p.PersonId = per.Id
        WHERE per.Email = :email
        """, nativeQuery = true)
    List<Object[]> findAuthDataByEmail(String email);
}