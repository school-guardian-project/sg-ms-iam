package com.school_guardian.ms_iam.application.usecase;

import com.school_guardian.ms_iam.infrastructure.persistence.entity.ProfileEntity;
import com.school_guardian.ms_iam.infrastructure.persistence.repository.ProfileJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InternalProfileServiceTest {
    private final ProfileJpaRepository repository = mock(ProfileJpaRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final InternalProfileService service = new InternalProfileService(repository, encoder, "57");
    private final UUID id = UUID.randomUUID();

    @Test
    void lookupNormalizesEmailAndReturnsContract() {
        when(repository.findActiveByEmail("person@example.com"))
            .thenReturn(Collections.singletonList(new Object[]{id.toString(), "person@example.com"}));
        var result = service.findByEmail(" Person@Example.com ");
        assertEquals(id, result.profileId());
        assertEquals("person@example.com", result.email());
    }

    @Test
    void missingProfileReturns404() {
        when(repository.findActiveByEmail("missing@example.com")).thenReturn(List.of());
        assertEquals(404, assertThrows(ResponseStatusException.class,
            () -> service.findByEmail("missing@example.com")).getStatusCode().value());
    }

    @Test
    void passwordIsEncodedByIam() {
        var profile = new ProfileEntity();
        profile.setStatus("Active");
        when(repository.findById(id)).thenReturn(Optional.of(profile));
        when(encoder.encode("TestPassword1!")).thenReturn("encoded-password");
        service.updatePassword(id, "TestPassword1!");
        assertEquals("encoded-password", profile.getPasswordHash());
        verify(repository).save(profile);
    }

    @Test
    void duplicateEmailIsRejectedWithoutWriting() {
        when(repository.findActiveByEmail("used@example.com"))
            .thenReturn(Collections.singletonList(new Object[]{UUID.randomUUID(), "used@example.com"}));
        assertEquals(409, assertThrows(ResponseStatusException.class,
            () -> service.updateEmail(id, "used@example.com")).getStatusCode().value());
        verify(repository, never()).updateEmail(any(), any());
    }

    @Test
    void phoneUsesLongNationalDigits() {
        when(repository.findActivePhone(id)).thenReturn(Optional.of(3001234567L));
        assertTrue(service.phoneMatches(id, "+573001234567"));
        assertFalse(service.phoneMatches(id, "+573001234568"));
        when(repository.updatePhone(id, 3001234567L)).thenReturn(1);
        service.updatePhone(id, "+573001234567");
        verify(repository).updatePhone(id, 3001234567L);
    }

    @Test
    void invalidPhoneIsRejectedWithoutWriting() {
        assertEquals(400, assertThrows(ResponseStatusException.class,
            () -> service.updatePhone(id, "3001234567")).getStatusCode().value());
        verify(repository, never()).updatePhone(any(), anyLong());
    }
}
