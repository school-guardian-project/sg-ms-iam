package com.school_guardian.ms_iam.application.usecase;

import com.school_guardian.ms_iam.domain.event.PersonCreatedEvent;
import com.school_guardian.ms_iam.domain.event.ProfileCreatedEvent;
import com.school_guardian.ms_iam.domain.exception.ProfileAlreadyExistsException;
import com.school_guardian.ms_iam.domain.model.Profile;
import com.school_guardian.ms_iam.domain.model.Role;
import com.school_guardian.ms_iam.domain.port.in.CreateProfileUseCase;
import com.school_guardian.ms_iam.domain.port.out.DomainEventPublisher;
import com.school_guardian.ms_iam.domain.port.out.ProfileRepository;
import com.school_guardian.ms_iam.domain.port.out.RoleRepository;
import com.school_guardian.ms_iam.shared.Status;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateProfileService implements CreateProfileUseCase {
    private static final String PROFILE_CREATED_TOPIC = "profile.created";

    private final ProfileRepository profileRepository;
    private final RoleRepository roleRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void execute(PersonCreatedEvent event, String roleName) {
        if (profileRepository.existsByPersonId(event.getPersonId())) {
            throw new ProfileAlreadyExistsException(event.getPersonId());
        }

        Role role = roleRepository.findByName(roleName);

        Profile profile = new Profile();
        profile.setId(UUID.randomUUID());
        profile.setPersonId(event.getPersonId());
        profile.setPasswordHash(passwordEncoder.encode(event.getIdentificationNumber()));
        profile.setRole(role);
        profile.setStatus(Status.Active);

        profileRepository.save(profile);
        if ("Admin".equals(roleName) && event.getSchoolId() != null) {
            profileRepository.assignSchoolByPersonId(profile.getPersonId(), event.getSchoolId());
        }

        ProfileCreatedEvent profileCreatedEvent = new ProfileCreatedEvent(
                UUID.randomUUID(),
                profile.getId(),
                profile.getPersonId(),
                role.getId()
        );

        domainEventPublisher.publish(profileCreatedEvent, "profile.created");
    }
}
