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
import com.school_guardian.ms_iam.domain.port.out.SchoolDirectory;
import com.school_guardian.ms_iam.shared.Status;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
    private final SchoolDirectory schoolDirectory;

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
        // Sede. Llega en student.created / driver.created / parent.created y es null
        // para admin.created, que se relaciona con un colegio y no con una sede.
        profile.setCampusId(event.getCampusId());

        profileRepository.save(profile);

        ProfileCreatedEvent profileCreatedEvent = new ProfileCreatedEvent(
                UUID.randomUUID(),
                profile.getId(),
                profile.getPersonId(),
                role.getId()
        );

        domainEventPublisher.publish(profileCreatedEvent, "profile.created");

        if (event.getSchoolId() != null) {
            linkAdminToSchoolAfterCommit(profile.getId(), event.getSchoolId());
        }
    }

    /**
     * Registra la relacion perfil-colegio <em>despues</em> del commit.
     *
     * <p>El orden no es estetico: {@code School.SchoolAdmin.ProfileId} tiene clave
     * foranea a {@code Iam.Profile.Id}. Si se llamara al gRPC dentro de la
     * transaccion, ms-school-management insertaria la fila antes de que exista el
     * perfil y la FK lo rechazaria. Despues del commit, el perfil ya es visible.
     *
     * <p>Ademas, si el enlace falla no se revierte nada: el usuario queda con su
     * cuenta creada, que es lo que no se puede perder. La relacion admin-colegio
     * queda pendiente y el admin entra sin {@code schoolId} en el token hasta que
     * se resuelva.
     */
    private void linkAdminToSchoolAfterCommit(UUID profileId, UUID schoolId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                if (!schoolDirectory.linkAdminSchool(profileId, schoolId)) {
                    log.warn(
                        "Admin profile {} was created but could not be linked to school {}; "
                        + "it will authenticate without a schoolId claim until the link is retried",
                        profileId, schoolId);
                }
            }
        });
    }
}
