package com.school_guardian.ms_iam.infrastructure.persistence.repository;

import com.school_guardian.ms_iam.infrastructure.persistence.entity.ProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProfileJpaRepository extends JpaRepository<ProfileEntity, UUID> {
    boolean existsByPersonId(UUID personId);
    Optional<ProfileEntity> findByPersonId(UUID personId);

    @Query(value = """
        SELECT p.Id, p.PersonId, p.PasswordHash, p.RoleId, p.Status, per.Email,
               p.CampuseId, COALESCE(sa.SchoolId, sc.SchoolId) AS SchoolId
        FROM Iam.Profile p
        INNER JOIN UserManagement.Person per ON p.PersonId = per.Id
        LEFT JOIN School.SchoolAdmin sa ON sa.ProfileId = p.Id AND sa.Status = 'Active'
        LEFT JOIN School.SchoolCampus sc ON sc.Id = p.CampuseId
        WHERE per.Email = :email
        """, nativeQuery = true)
    List<Object[]> findAuthDataByEmail(String email);

    @Query(value = """
        SELECT p.Id, p.PersonId, p.PasswordHash, p.RoleId, p.Status, per.Email,
               p.CampuseId, COALESCE(sa.SchoolId, sc.SchoolId) AS SchoolId
        FROM Iam.Profile p
        INNER JOIN UserManagement.Person per ON p.PersonId = per.Id
        LEFT JOIN School.SchoolAdmin sa ON sa.ProfileId = p.Id AND sa.Status = 'Active'
        LEFT JOIN School.SchoolCampus sc ON sc.Id = p.CampuseId
        WHERE p.Id = :profileId
        """, nativeQuery = true)
    List<Object[]> findAuthDataByProfileId(UUID profileId);

    @Query(value = """
        SELECT p.Id, p.PersonId, per.Email, p.RoleId, r.Name, p.CampuseId, sc.Name,
               COALESCE(sa.SchoolId, sc.SchoolId), s.Name, per.Name, per.LastName, p.Status,
               ci.Name
        FROM Iam.Profile p
        INNER JOIN UserManagement.Person per ON per.Id = p.PersonId
        INNER JOIN Iam.Role r ON r.Id = p.RoleId
        LEFT JOIN School.SchoolAdmin sa ON sa.ProfileId = p.Id AND sa.Status = 'Active'
        LEFT JOIN School.SchoolCampus sc ON sc.Id = p.CampuseId
        LEFT JOIN School.School s ON s.Id = COALESCE(sa.SchoolId, sc.SchoolId)
        LEFT JOIN Geographic.City ci ON ci.Id = s.CityId
        WHERE p.Id = :profileId
        """, nativeQuery = true)
    List<Object[]> findProfileViewById(UUID profileId);
}