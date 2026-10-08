package com.school_guardian.ms_iam.infrastructure.persistence.repository;

import com.school_guardian.ms_iam.application.dto.AuthenticationData;
import com.school_guardian.ms_iam.domain.port.in.AuthenticationRepository;
import com.school_guardian.ms_iam.infrastructure.persistence.entity.ProfileEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class AuthenticationRepositoryImpl implements AuthenticationRepository {

    private final ProfileJpaRepository profileJpaRepository;

    public AuthenticationRepositoryImpl(ProfileJpaRepository profileJpaRepository) {
        this.profileJpaRepository = profileJpaRepository;
    }

    @Override
    public Optional<AuthenticationData> findByPersonId(UUID personId) {
        return profileJpaRepository.findByPersonId(personId)
            .map(this::toAuthenticationData);
    }

    public Optional<AuthenticationData> findByEmail(String email) {
        return profileJpaRepository.findAuthDataByEmail(email)
            .stream()
            .findFirst()
            .map(this::toAuthenticationDataFromJoin);
    }

    @Override
    public Optional<AuthenticationData> findByProfileId(UUID profileId) {
        return profileJpaRepository.findAuthDataByProfileId(profileId)
            .stream()
            .findFirst()
            .map(this::toAuthenticationDataFromJoin);
    }

    private AuthenticationData toAuthenticationData(ProfileEntity entity) {
        AuthenticationData data = new AuthenticationData();
        data.profileId = entity.getId();
        data.personId = entity.getPersonId();
        data.passwordHash = entity.getPasswordHash();
        data.roleId = entity.getRoleId();
        data.status = entity.getStatus();
        return data;
    }

    private AuthenticationData toAuthenticationDataFromJoin(Object[] row) {
        AuthenticationData data = new AuthenticationData();
        data.profileId = toUuid(row[0]);
        data.personId = toUuid(row[1]);
        data.passwordHash = (String) row[2];
        data.roleId = row[3] != null ? ((Number) row[3]).byteValue() : null;
        data.status = (String) row[4];
        data.email = (String) row[5];
        data.campusId = row[6] != null ? toUuid(row[6]) : null;
        data.schoolId = row[7] != null ? toUuid(row[7]) : null;
        return data;
    }

    private UUID toUuid(Object value) {
        if (value instanceof UUID uuid) return uuid;
        return UUID.fromString(value.toString());
    }
}