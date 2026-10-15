package com.school_guardian.ms_iam.application.usecase;

import com.school_guardian.ms_iam.infrastructure.persistence.entity.ProfileEntity;
import com.school_guardian.ms_iam.infrastructure.persistence.repository.ProfileJpaRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class UpdateProfileStatusServiceTest {

    private final ProfileJpaRepository repository = mock(ProfileJpaRepository.class);
    private final UpdateProfileStatusService service = new UpdateProfileStatusService(repository);
    private final UUID personId = UUID.randomUUID();

    private ProfileEntity profile(String status) {
        return new ProfileEntity(UUID.randomUUID(), personId, null, "hash", (byte) 1, status);
    }

    @Test
    void appliesStatusToProfile() {
        ProfileEntity entity = profile("Active");
        when(repository.findByPersonId(personId)).thenReturn(Optional.of(entity));

        service.execute(personId, "Inactive");

        verify(repository).save(entity);
        assertEquals("Inactive", entity.getStatus());
    }

    @Test
    void isIdempotentForSameStatus() {
        ProfileEntity entity = profile("Active");
        when(repository.findByPersonId(personId)).thenReturn(Optional.of(entity));

        service.execute(personId, "Active");

        verify(repository, never()).save(any());
    }

    @Test
    void ignoresUnknownStatus() {
        service.execute(personId, "Deleted");

        verifyNoInteractions(repository);
    }

    @Test
    void ignoresMissingProfile() {
        when(repository.findByPersonId(personId)).thenReturn(Optional.empty());

        service.execute(personId, "Inactive");

        verify(repository, never()).save(any());
    }
}
