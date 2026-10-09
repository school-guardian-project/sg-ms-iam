package com.school_guardian.ms_iam.application.usecase;

import com.school_guardian.ms_iam.domain.event.PersonCreatedEvent;
import com.school_guardian.ms_iam.domain.model.Profile;
import com.school_guardian.ms_iam.domain.model.Role;
import com.school_guardian.ms_iam.domain.port.out.DomainEventPublisher;
import com.school_guardian.ms_iam.domain.port.out.ProfileRepository;
import com.school_guardian.ms_iam.domain.port.out.RoleRepository;
import com.school_guardian.ms_iam.infrastructure.persistence.entity.ProfileEntity;
import com.school_guardian.ms_iam.infrastructure.persistence.repository.ProfileJpaRepository;
import com.school_guardian.ms_iam.infrastructure.persistence.repository.ProfileRepositoryImpl;
import com.school_guardian.ms_iam.shared.Status;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CreateProfileSchoolAssociationTest {
    private final ProfileRepository profiles = mock(ProfileRepository.class);
    private final RoleRepository roles = mock(RoleRepository.class);
    private final DomainEventPublisher events = mock(DomainEventPublisher.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final CreateProfileService service = new CreateProfileService(profiles, roles, events, encoder);

    @ParameterizedTest
    @ValueSource(strings = {"Student", "Driver", "Parent"})
    void regularUsersKeepTheirCampusWithoutCreatingAdminAssignment(String roleName) {
        var event = event();
        event.setCampusId(UUID.randomUUID());
        when(roles.findByName(roleName)).thenReturn(new Role((byte) 2, roleName, "", Status.Active));
        service.execute(event, roleName);
        var saved = ArgumentCaptor.forClass(Profile.class);
        verify(profiles).save(saved.capture());
        assertEquals(event.getCampusId(), saved.getValue().getCampusId());
        verify(profiles, never()).assignSchoolByPersonId(any(), any());
    }

    @Test
    void adminKeepsExistingSchoolAdminAssociation() {
        var event = event();
        event.setSchoolId(UUID.randomUUID());
        when(roles.findByName("Admin")).thenReturn(new Role((byte) 1, "Admin", "", Status.Active));
        service.execute(event, "Admin");
        verify(profiles).assignSchoolByPersonId(event.getPersonId(), event.getSchoolId());
    }

    @Test
    void repositoryMapsCampusToExistingProfileColumn() {
        var jpa = mock(ProfileJpaRepository.class);
        when(jpa.save(any(ProfileEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var profile = new Profile();
        profile.setId(UUID.randomUUID());
        profile.setPersonId(UUID.randomUUID());
        profile.setCampusId(UUID.randomUUID());
        profile.setRole(new Role((byte) 2, "Student", "", Status.Active));
        profile.setStatus(Status.Active);
        new ProfileRepositoryImpl(jpa).save(profile);
        var entity = ArgumentCaptor.forClass(ProfileEntity.class);
        verify(jpa).save(entity.capture());
        assertEquals(profile.getCampusId(), entity.getValue().getCampusId());
    }

    private PersonCreatedEvent event() {
        var event = new PersonCreatedEvent();
        event.setPersonId(UUID.randomUUID());
        event.setIdentificationNumber("123456");
        when(encoder.encode(anyString())).thenReturn("hashed");
        return event;
    }
}
