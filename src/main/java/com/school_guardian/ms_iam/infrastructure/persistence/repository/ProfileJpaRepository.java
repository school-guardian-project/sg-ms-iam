package com.school_guardian.ms_iam.infrastructure.persistence.repository;

import com.school_guardian.ms_iam.infrastructure.persistence.entity.ProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProfileJpaRepository extends JpaRepository<ProfileEntity, UUID> {
    boolean existsByPersonId(UUID personId);
    Optional<ProfileEntity> findByPersonId(UUID personId);

    @Query(value = """
        SELECT p.Id, p.PersonId, p.PasswordHash, p.RoleId, p.Status, per.Email
        FROM Iam.Profile p
        INNER JOIN UserManagement.Person per ON p.PersonId = per.Id
        WHERE per.Email = :email
        """, nativeQuery = true)
    List<Object[]> findAuthDataByEmail(String email);

    @Query(value = """
        SELECT p.Id, per.Email
        FROM Iam.Profile p
        INNER JOIN UserManagement.Person per ON p.PersonId = per.Id
        WHERE per.Email = :email AND p.Status = 'Active'
        """, nativeQuery = true)
    List<Object[]> findActiveLookupByEmail(String email);

    @Query(value = """
        SELECT p.Id, CAST(per.Phone AS VARCHAR(20))
        FROM Iam.Profile p
        INNER JOIN UserManagement.Person per ON p.PersonId = per.Id
        WHERE per.Phone = :nationalPhone AND p.Status = 'Active'
        """, nativeQuery = true)
    List<Object[]> findActiveLookupByPhone(long nationalPhone);

    @Modifying
    @Transactional
    @Query("UPDATE ProfileEntity p SET p.passwordHash = :passwordHash WHERE p.id = :id")
    int updatePasswordHash(UUID id, String passwordHash);

    @Modifying
    @Transactional
    @Query(value = """
        UPDATE per SET per.Email = :email
        FROM UserManagement.Person per
        INNER JOIN Iam.Profile p ON p.PersonId = per.Id
        WHERE p.Id = :id
        """, nativeQuery = true)
    int updateEmail(UUID id, String email);

    @Modifying
    @Transactional
    @Query(value = """
        UPDATE per SET per.Phone = :phone
        FROM UserManagement.Person per
        INNER JOIN Iam.Profile p ON p.PersonId = per.Id
        WHERE p.Id = :id
        """, nativeQuery = true)
    int updatePhone(UUID id, long phone);

    @Query(value = """
        SELECT per.Phone
        FROM UserManagement.Person per
        INNER JOIN Iam.Profile p ON p.PersonId = per.Id
        WHERE p.Id = :id
        """, nativeQuery = true)
    List<Long> findPhoneByProfileId(UUID id);
}