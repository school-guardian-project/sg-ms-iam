package com.school_guardian.ms_iam.infrastructure.persistence.repository;

import com.school_guardian.ms_iam.infrastructure.persistence.entity.ProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProfileJpaRepository extends JpaRepository<ProfileEntity, UUID> {
    boolean existsByPersonId(UUID personId);
    Optional<ProfileEntity> findByPersonId(UUID personId);

    @Query(value = """
        SELECT p.Id, per.Email FROM Iam.Profile p
        JOIN UserManagement.Person per ON per.Id = p.PersonId
        WHERE LOWER(per.Email) = :email AND p.Status = 'Active' AND per.Status = 'Active'
        """, nativeQuery = true)
    List<Object[]> findActiveByEmail(String email);

    @Query(value = """
        SELECT per.Phone FROM UserManagement.Person per
        JOIN Iam.Profile p ON p.PersonId = per.Id
        WHERE p.Id = :profileId AND p.Status = 'Active' AND per.Status = 'Active'
        """, nativeQuery = true)
    Optional<Long> findActivePhone(UUID profileId);

    @Modifying
    @Query(value = """
        UPDATE per SET Email = :email FROM UserManagement.Person per
        JOIN Iam.Profile p ON p.PersonId = per.Id
        WHERE p.Id = :profileId AND p.Status = 'Active' AND per.Status = 'Active'
        """, nativeQuery = true)
    int updateEmail(UUID profileId, String email);

    @Modifying
    @Query(value = """
        UPDATE per SET Phone = :phone FROM UserManagement.Person per
        JOIN Iam.Profile p ON p.PersonId = per.Id
        WHERE p.Id = :profileId AND p.Status = 'Active' AND per.Status = 'Active'
        """, nativeQuery = true)
    int updatePhone(UUID profileId, long phone);

    @Modifying
    @Query(value = """
        UPDATE School.SchoolAdmin
        SET SchoolId = :schoolId, Status = 'Active'
        WHERE ProfileId = :profileId
        """, nativeQuery = true)
    int updateSchoolAssignment(UUID profileId, UUID schoolId);

    @Modifying
    @Query(value = """
        INSERT INTO School.SchoolAdmin (Id, SchoolId, ProfileId, Status)
        VALUES (NEWID(), :schoolId, :profileId, 'Active')
        """, nativeQuery = true)
    int createSchoolAssignment(UUID profileId, UUID schoolId);

    @Query(value = """
        SELECT p.Id, p.PersonId, p.PasswordHash, p.RoleId, p.Status, per.Email,
               p.CampuseId, COALESCE(sa.SchoolId, sc.SchoolId) AS SchoolId,
               r.Name AS RoleName, per.IdentificationNumber
        FROM Iam.Profile p
        INNER JOIN UserManagement.Person per ON p.PersonId = per.Id
        INNER JOIN Iam.Role r ON r.Id = p.RoleId
        LEFT JOIN School.SchoolAdmin sa ON sa.ProfileId = p.Id AND sa.Status = 'Active'
        LEFT JOIN School.SchoolCampus sc ON sc.Id = p.CampuseId
        WHERE per.Email = :email
        """, nativeQuery = true)
    List<Object[]> findAuthDataByEmail(String email);

    @Query(value = """
        SELECT p.Id, p.PersonId, p.PasswordHash, p.RoleId, p.Status, per.Email,
               p.CampuseId, COALESCE(sa.SchoolId, sc.SchoolId) AS SchoolId,
               r.Name AS RoleName, per.IdentificationNumber
        FROM Iam.Profile p
        INNER JOIN UserManagement.Person per ON p.PersonId = per.Id
        INNER JOIN Iam.Role r ON r.Id = p.RoleId
        LEFT JOIN School.SchoolAdmin sa ON sa.ProfileId = p.Id AND sa.Status = 'Active'
        LEFT JOIN School.SchoolCampus sc ON sc.Id = p.CampuseId
        WHERE p.Id = :profileId
        """, nativeQuery = true)
    List<Object[]> findAuthDataByProfileId(UUID profileId);

    @Query(value = """
        SELECT pe.TypePermissions
        FROM Iam.PermissionsRole pr
        INNER JOIN Iam.Permissions pe ON pe.Id = pr.PermissionsId
        WHERE pr.RoleId = :roleId
        ORDER BY pe.TypePermissions
        """, nativeQuery = true)
    List<String> findPermissionNamesByRoleId(Byte roleId);

    @Query(value = """
        SELECT p.Id, p.PersonId, per.Email, p.RoleId, r.Name, p.CampuseId, sc.Name,
               COALESCE(sa.SchoolId, sc.SchoolId), s.Name, per.Name, per.LastName, p.Status,
               ci.Name, per.IdentificationNumber
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

    @Query(value = """
        SELECT COUNT(1) FROM Iam.Profile p
        WHERE p.Id = :profileId AND p.Status = 'Active'
        """, nativeQuery = true)
    long countActiveById(UUID profileId);

    @Query(value = """
        SELECT per.Name, per.LastName
        FROM Iam.Profile p
        INNER JOIN UserManagement.Person per ON per.Id = p.PersonId
        WHERE p.Id = :profileId AND p.Status = 'Active' AND per.Status = 'Active'
        """, nativeQuery = true)
    List<Object[]> findActivePersonName(UUID profileId);
}