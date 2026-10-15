package com.school_guardian.ms_iam.infrastructure.persistence.repository;

import com.school_guardian.ms_iam.application.dto.AuthenticationData;
import com.school_guardian.ms_iam.application.dto.ProfileView;
import com.school_guardian.ms_iam.domain.port.in.AuthenticationRepository;
import com.school_guardian.ms_iam.infrastructure.persistence.entity.ProfileEntity;
import org.springframework.stereotype.Component;

import java.util.List;
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

    @Override
    public Optional<AuthenticationData> findByEmail(String email) {
        return profileJpaRepository.findAuthDataByEmail(email)
            .stream()
            .findFirst()
            .map(this::toAuthenticationDataFromJoin)
            .map(this::withPermissions);
    }

    @Override
    public Optional<AuthenticationData> findByProfileId(UUID profileId) {
        return profileJpaRepository.findAuthDataByProfileId(profileId)
            .stream()
            .findFirst()
            .map(this::toAuthenticationDataFromJoin)
            .map(this::withPermissions);
    }

    @Override
    public List<String> findPermissionNamesByRoleId(Byte roleId) {
        if (roleId == null) return List.of();
        return profileJpaRepository.findPermissionNamesByRoleId(roleId);
    }

    private AuthenticationData withPermissions(AuthenticationData data) {
        data.permissions = findPermissionNamesByRoleId(data.roleId);
        return data;
    }

    @Override
    public Optional<ProfileView> findProfileViewById(UUID profileId) {
        return profileJpaRepository.findProfileViewById(profileId)
            .stream()
            .findFirst()
            .map(this::toProfileView);
    }

    private ProfileView toProfileView(Object[] row) {
        ProfileView view = new ProfileView();
        view.profileId = toUuid(row[0]);
        view.personId = toUuid(row[1]);
        view.email = (String) row[2];
        view.roleId = row[3] != null ? ((Number) row[3]).byteValue() : null;
        view.roleName = (String) row[4];
        view.campusId = row[5] != null ? toUuid(row[5]) : null;
        view.campusName = (String) row[6];
        view.schoolId = row[7] != null ? toUuid(row[7]) : null;
        view.schoolName = (String) row[8];
        view.name = (String) row[9];
        view.lastName = (String) row[10];
        view.status = (String) row[11];
        view.cityName = (String) row[12];
        view.identificationNumber = (String) row[13];
        return view;
    }

    private AuthenticationData toAuthenticationData(ProfileEntity entity) {
        AuthenticationData data = new AuthenticationData();
        data.profileId = entity.getId();
        data.personId = entity.getPersonId();
        data.campusId = entity.getCampusId();
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
        data.roleName = (String) row[8];
        data.identificationNumber = (String) row[9];
        return data;
    }

    private UUID toUuid(Object value) {
        if (value instanceof UUID uuid) return uuid;
        return UUID.fromString(value.toString());
    }
}