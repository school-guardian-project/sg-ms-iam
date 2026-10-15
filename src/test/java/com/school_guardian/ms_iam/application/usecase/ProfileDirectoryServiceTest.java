package com.school_guardian.ms_iam.application.usecase;

import com.school_guardian.ms_iam.infrastructure.persistence.repository.ProfileJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProfileDirectoryServiceTest {

    private final ProfileJpaRepository repository = mock(ProfileJpaRepository.class);
    private final ProfileDirectoryService service = new ProfileDirectoryService(repository);
    private final UUID id = UUID.randomUUID();

    @Test
    void existsIsTrueForAnActiveProfile() {
        when(repository.countActiveById(id)).thenReturn(1L);
        assertTrue(service.exists(id));
    }

    @Test
    void existsIsFalseForANonActiveOrUnknownProfile() {
        when(repository.countActiveById(id)).thenReturn(0L);
        assertFalse(service.exists(id));
    }

    @Test
    void nameJoinsFirstAndLastName() {
        when(repository.findActivePersonName(id))
            .thenReturn(Collections.singletonList(new Object[]{"Ana", "Pérez"}));
        assertEquals("Ana Pérez", service.name(id));
    }

    @Test
    void nameTrimsWhenLastNameIsMissing() {
        when(repository.findActivePersonName(id))
            .thenReturn(Collections.singletonList(new Object[]{"Ana", null}));
        assertEquals("Ana", service.name(id));
    }

    @Test
    void missingProfileOnNameReturns404() {
        when(repository.findActivePersonName(id)).thenReturn(List.of());
        assertEquals(404, assertThrows(ResponseStatusException.class,
            () -> service.name(id)).getStatusCode().value());
    }
}
