package com.school_guardian.ms_iam.infrastructure.persistence.repository;

import com.school_guardian.ms_iam.domain.model.Profile;
import com.school_guardian.ms_iam.domain.port.out.ProfileRepository;
import com.school_guardian.ms_iam.infrastructure.persistence.entity.ProfileEntity;
import org.springframework.stereotype.Component;

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
        entity.setCampusId(profile.getCampusId());

        ProfileEntity saved = profileJpaRepository.save(entity);
        profile.setId(saved.getId());
        return profile;
    }
}
