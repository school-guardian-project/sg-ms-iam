package com.school_guardian.ms_iam.infrastructure.persistence.repository;

import com.school_guardian.ms_iam.domain.model.PhoneLookup;
import com.school_guardian.ms_iam.domain.model.Profile;
import com.school_guardian.ms_iam.domain.model.ProfileLookup;
import com.school_guardian.ms_iam.domain.port.out.ProfileRepository;
import com.school_guardian.ms_iam.infrastructure.persistence.entity.ProfileEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class ProfileRepositoryImpl implements ProfileRepository {
    private final ProfileJpaRepository profileJpaRepository;

    public ProfileRepositoryImpl(ProfileJpaRepository profileJpaRepository) {
        this.profileJpaRepository = profileJpaRepository;
    }

    @Override
    public boolean existsByPersonId(UUID personId) {
        return profileJpaRepository.existsByPersonId(personId);
    }

    @Override
    public Profile save(Profile profile) {
        ProfileEntity entity = new ProfileEntity();
        entity.setId(profile.getId());
        entity.setPersonId(profile.getPersonId());
        entity.setPasswordHash(profile.getPasswordHash());
        entity.setRoleId(profile.getRole().getId());
        entity.setStatus(profile.getStatus().name());

        ProfileEntity saved = profileJpaRepository.save(entity);
        profile.setId(saved.getId());
        return profile;
    }

    @Override
    public Optional<ProfileLookup> findActiveLookupByEmail(String email) {
        return profileJpaRepository.findActiveLookupByEmail(email).stream()
                .findFirst()
                .map(row -> new ProfileLookup(toUuid(row[0]), (String) row[1]));
    }

    @Override
    public Optional<PhoneLookup> findActiveLookupByPhone(long nationalPhone) {
        return profileJpaRepository.findActiveLookupByPhone(nationalPhone).stream()
                .findFirst()
                .map(row -> new PhoneLookup(toUuid(row[0]), (String) row[1]));
    }

    @Override
    public boolean updatePasswordHash(UUID profileId, String passwordHash) {
        return profileJpaRepository.updatePasswordHash(profileId, passwordHash) > 0;
    }

    @Override
    public boolean updateEmail(UUID profileId, String email) {
        return profileJpaRepository.updateEmail(profileId, email) > 0;
    }

    @Override
    public boolean updatePhone(UUID profileId, long phone) {
        return profileJpaRepository.updatePhone(profileId, phone) > 0;
    }

    @Override
    public java.util.Optional<Long> findPhoneByProfileId(UUID profileId) {
        return profileJpaRepository.findPhoneByProfileId(profileId).stream().findFirst();
    }

    private UUID toUuid(Object value) {
        if (value instanceof UUID uuid) return uuid;
        return UUID.fromString(value.toString());
    }}
