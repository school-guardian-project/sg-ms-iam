package com.school_guardian.ms_iam.infrastructure.persistence.repository;

import com.school_guardian.ms_iam.application.dto.AuthenticationData;
import com.school_guardian.ms_iam.domain.port.in.AuthenticationRepository;
import com.school_guardian.ms_iam.infrastructure.persistence.entity.ProfileEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
public class AuthenticationRepositoryImpl implements AuthenticationRepository {

    private final ProfileJpaRepository profileJpaRepository;

    public AuthenticationRepositoryImpl(ProfileJpaRepository profileJpaRepository) {
        this.profileJpaRepository = profileJpaRepository;
    }

    @Override
    public Optional<AuthenticationData> findByEmail(String email) {
        return profileJpaRepository.findAuthDataByEmail(email)
            .stream()
            .findFirst()
            .map(this::toAuthenticationDataFromJoin);
    }

    @Override
    public Optional<AuthenticationData> findByProfileId(UUID profileId) {
        return profileJpaRepository.findById(profileId)
            .map(this::toAuthenticationData);
    }

    @Override
    @Transactional
    public void updatePassword(UUID profileId, String newPasswordHash) {
        profileJpaRepository.updatePasswordHash(profileId, newPasswordHash);
    }

    private AuthenticationData toAuthenticationData(ProfileEntity entity) {
        AuthenticationData data = new AuthenticationData();
        data.profileId = entity.getId();
        data.personId = entity.getPersonId();
        data.passwordHash = entity.getPasswordHash();
        data.roleId = entity.getRoleId();
        data.status = entity.getStatus();
        data.campusId = entity.getCampusId();
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
        // La sede viene de la propia fila de Profile: no hace falta otra ida a
        // ms-school-management para el login, porque aqui ya esta el dato.
        data.campusId = row[6] != null ? toUuid(row[6]) : null;
        return data;
    }

    private UUID toUuid(Object value) {
        if (value instanceof UUID uuid) return uuid;
        return UUID.fromString(value.toString());
    }
}