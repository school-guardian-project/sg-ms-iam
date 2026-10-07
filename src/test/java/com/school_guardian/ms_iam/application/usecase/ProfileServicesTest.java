package com.school_guardian.ms_iam.application.usecase;

import com.school_guardian.ms_iam.domain.exception.EmailAlreadyInUseException;
import com.school_guardian.ms_iam.domain.exception.ProfileNotFoundException;
import com.school_guardian.ms_iam.domain.model.PhoneLookup;
import com.school_guardian.ms_iam.domain.model.ProfileLookup;
import com.school_guardian.ms_iam.domain.port.in.MatchProfilePhoneUseCase;
import com.school_guardian.ms_iam.domain.port.in.UpdateProfileEmailUseCase;
import com.school_guardian.ms_iam.domain.port.in.UpdateProfilePhoneUseCase;
import com.school_guardian.ms_iam.domain.port.in.UpdateProfilePasswordUseCase.UpdatePassword;
import com.school_guardian.ms_iam.domain.port.out.ProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProfileServicesTest {

    private final ProfileRepository repository = mock(ProfileRepository.class);
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    void findByEmailReturnsProfile() {
        var id = UUID.randomUUID();
        when(repository.findActiveLookupByEmail("user@gmail.com"))
            .thenReturn(Optional.of(new ProfileLookup(id, "user@gmail.com")));

        var result = new FindProfileByEmailService(repository).execute(" user@gmail.com ");

        assertEquals(id, result.profileId());
        assertEquals("user@gmail.com", result.email());
    }

    @Test
    void findByEmailThrowsWhenUnknown() {
        when(repository.findActiveLookupByEmail(any())).thenReturn(Optional.empty());

        assertThrows(ProfileNotFoundException.class,
            () -> new FindProfileByEmailService(repository).execute("nobody@gmail.com"));
    }

    @Test
    void findByEmailNormalizesCaseAndWhitespace() {
        var id = UUID.randomUUID();
        when(repository.findActiveLookupByEmail("user@gmail.com"))
            .thenReturn(Optional.of(new ProfileLookup(id, "user@gmail.com")));

        var result = new FindProfileByEmailService(repository).execute("  User@Gmail.COM ");

        assertEquals(id, result.profileId());
    }

    @Test
    void findByPhoneAcceptsE164NationalAndPlainCountryCode() {
        var id = UUID.randomUUID();
        when(repository.findActiveLookupByPhone(3001234567L))
            .thenReturn(Optional.of(new PhoneLookup(id, "3001234567")));

        var service = new FindProfileByPhoneService(repository, "57");

        assertEquals(id, service.execute("+57 300 123 4567").profileId());
        assertEquals(id, service.execute("573001234567").profileId());
        assertEquals(id, service.execute("3001234567").profileId());
    }

    @Test
    void findByPhoneThrowsWhenUnknown() {
        when(repository.findActiveLookupByPhone(any(Long.class))).thenReturn(Optional.empty());

        assertThrows(ProfileNotFoundException.class,
            () -> new FindProfileByPhoneService(repository, "57").execute("+573009999999"));
    }

    @Test
    void findByPhoneRejectsInvalidInput() {
        var service = new FindProfileByPhoneService(repository, "57");

        assertThrows(IllegalArgumentException.class, () -> service.execute("abc"));
        assertThrows(IllegalArgumentException.class, () -> service.execute(""));
    }

    @Test
    void updatePasswordStoresBcryptHashNeverPlaintext() {
        var id = UUID.randomUUID();
        when(repository.updatePasswordHash(eq(id), any())).thenReturn(true);

        new UpdateProfilePasswordService(repository, encoder).execute(new UpdatePassword(id, "NewPassw0rd!"));

        var captor = forClass(String.class);
        verify(repository).updatePasswordHash(eq(id), captor.capture());
        assertNotEquals("NewPassw0rd!", captor.getValue());
        assertTrue(encoder.matches("NewPassw0rd!", captor.getValue()));
    }

    @Test
    void updateEmailNormalizesAndStoresNewAddress() {
        var id = UUID.randomUUID();
        when(repository.findActiveLookupByEmail("new@gmail.com")).thenReturn(Optional.empty());
        when(repository.updateEmail(id, "new@gmail.com")).thenReturn(true);

        new UpdateProfileEmailService(repository).execute(new UpdateProfileEmailUseCase.UpdateEmail(id, "  New@Gmail.COM "));

        verify(repository).updateEmail(id, "new@gmail.com");
    }

    @Test
    void updateEmailRejectsAddressOwnedByAnotherProfile() {
        var other = UUID.randomUUID();
        when(repository.findActiveLookupByEmail("taken@gmail.com"))
            .thenReturn(Optional.of(new ProfileLookup(other, "taken@gmail.com")));

        assertThrows(EmailAlreadyInUseException.class,
            () -> new UpdateProfileEmailService(repository)
                .execute(new UpdateProfileEmailUseCase.UpdateEmail(UUID.randomUUID(), "taken@gmail.com")));
        verify(repository, never()).updateEmail(any(), any());
    }

    @Test
    void updateEmailThrowsWhenProfileMissing() {
        when(repository.findActiveLookupByEmail(any())).thenReturn(Optional.empty());
        when(repository.updateEmail(any(), any())).thenReturn(false);

        assertThrows(ProfileNotFoundException.class,
            () -> new UpdateProfileEmailService(repository)
                .execute(new UpdateProfileEmailUseCase.UpdateEmail(UUID.randomUUID(), "new@gmail.com")));
    }

    @Test
    void matchPhoneComparesNormalizedNumberWithStoredOne() {
        var id = UUID.randomUUID();
        when(repository.findPhoneByProfileId(id)).thenReturn(Optional.of(3001234567L));
        var service = new MatchProfilePhoneService(repository, "57");

        assertTrue(service.execute(new MatchProfilePhoneUseCase.MatchPhone(id, "+573001234567")));
        assertFalse(service.execute(new MatchProfilePhoneUseCase.MatchPhone(id, "+573009999999")));
    }

    @Test
    void matchPhoneThrowsWhenProfileMissing() {
        when(repository.findPhoneByProfileId(any())).thenReturn(Optional.empty());

        assertThrows(ProfileNotFoundException.class,
            () -> new MatchProfilePhoneService(repository, "57")
                .execute(new MatchProfilePhoneUseCase.MatchPhone(UUID.randomUUID(), "+573001234567")));
    }

    @Test
    void updatePhoneStoresNationalNumberFromE164() {        var id = UUID.randomUUID();
        when(repository.updatePhone(id, 3001234567L)).thenReturn(true);

        new UpdateProfilePhoneService(repository, "57").execute(new UpdateProfilePhoneUseCase.UpdatePhone(id, "+573001234567"));

        verify(repository).updatePhone(id, 3001234567L);
    }

    @Test
    void updatePhoneRejectsInvalidInputWithoutTouchingTheRepository() {
        assertThrows(IllegalArgumentException.class,
            () -> new UpdateProfilePhoneService(repository, "57")
                .execute(new UpdateProfilePhoneUseCase.UpdatePhone(UUID.randomUUID(), "abc")));
        verify(repository, never()).updatePhone(any(), org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void updatePhoneThrowsWhenProfileMissing() {
        when(repository.updatePhone(any(), org.mockito.ArgumentMatchers.anyLong())).thenReturn(false);

        assertThrows(ProfileNotFoundException.class,
            () -> new UpdateProfilePhoneService(repository, "57")
                .execute(new UpdateProfilePhoneUseCase.UpdatePhone(UUID.randomUUID(), "3001234567")));
    }

    @Test
    void updatePasswordThrowsWhenProfileMissing() {        when(repository.updatePasswordHash(any(), any())).thenReturn(false);

        assertThrows(ProfileNotFoundException.class,
            () -> new UpdateProfilePasswordService(repository, encoder)
                .execute(new UpdatePassword(UUID.randomUUID(), "NewPassw0rd!")));
    }
}