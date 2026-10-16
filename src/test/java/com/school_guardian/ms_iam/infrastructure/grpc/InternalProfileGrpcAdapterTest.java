package com.school_guardian.ms_iam.infrastructure.grpc;

import com.school_guardian.ms_iam.application.usecase.InternalProfileService;
import com.school_guardian.ms_iam.infrastructure.grpc.contract.FindProfileByEmailRequest;
import com.school_guardian.ms_iam.infrastructure.grpc.contract.PhoneMatchesRequest;
import com.school_guardian.ms_iam.infrastructure.grpc.contract.PhoneMatchesResponse;
import com.school_guardian.ms_iam.infrastructure.grpc.contract.ProfileLookup;
import com.school_guardian.ms_iam.infrastructure.grpc.contract.UpdateEmailRequest;
import com.school_guardian.ms_iam.infrastructure.grpc.contract.UpdateEmailResponse;
import com.school_guardian.ms_iam.infrastructure.grpc.contract.UpdatePasswordRequest;
import com.school_guardian.ms_iam.infrastructure.grpc.contract.UpdatePasswordResponse;
import com.school_guardian.ms_iam.infrastructure.grpc.contract.UpdatePhoneRequest;
import com.school_guardian.ms_iam.infrastructure.grpc.contract.UpdatePhoneResponse;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InternalProfileGrpcAdapterTest {

    private final InternalProfileService service = mock(InternalProfileService.class);
    private final InternalProfileGrpcAdapter adapter = new InternalProfileGrpcAdapter(service);
    private final UUID profileId = UUID.randomUUID();

    @SuppressWarnings("unchecked")
    private <T> StreamObserver<T> observer() {
        return mock(StreamObserver.class);
    }

    private static <T> StatusRuntimeException errorOf(StreamObserver<T> observer) {
        ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(observer).onError(captor.capture());
        assertInstanceOf(StatusRuntimeException.class, captor.getValue());
        return (StatusRuntimeException) captor.getValue();
    }

    @Test
    void findProfileByEmailDelegatesAndMapsLookup() {
        when(service.findByEmail("person@example.com"))
            .thenReturn(new InternalProfileService.Lookup(profileId, "person@example.com"));
        StreamObserver<ProfileLookup> observer = observer();

        adapter.findProfileByEmail(
            FindProfileByEmailRequest.newBuilder().setEmail("person@example.com").build(), observer);

        verify(observer).onNext(ProfileLookup.newBuilder()
            .setProfileId(profileId.toString())
            .setEmail("person@example.com")
            .build());
        verify(observer).onCompleted();
    }

    @Test
    void findProfileByEmailRejectsInvalidEmail() {
        StreamObserver<ProfileLookup> observer = observer();

        adapter.findProfileByEmail(
            FindProfileByEmailRequest.newBuilder().setEmail("not-an-email").build(), observer);

        assertEquals(Status.INVALID_ARGUMENT.getCode(), errorOf(observer).getStatus().getCode());
        verifyNoInteractions(service);
    }

    @Test
    void findProfileByEmailMaps404ToNotFound() {
        when(service.findByEmail("missing@example.com"))
            .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Active profile not found"));
        StreamObserver<ProfileLookup> observer = observer();

        adapter.findProfileByEmail(
            FindProfileByEmailRequest.newBuilder().setEmail("missing@example.com").build(), observer);

        assertEquals(Status.NOT_FOUND.getCode(), errorOf(observer).getStatus().getCode());
    }

    @Test
    void phoneMatchesDelegatesAndReturnsFlag() {
        when(service.phoneMatches(profileId, "+573001234567")).thenReturn(true);
        StreamObserver<PhoneMatchesResponse> observer = observer();

        adapter.phoneMatches(PhoneMatchesRequest.newBuilder()
            .setProfileId(profileId.toString())
            .setPhoneE164("+573001234567")
            .build(), observer);

        verify(observer).onNext(PhoneMatchesResponse.newBuilder().setMatches(true).build());
        verify(observer).onCompleted();
    }

    @Test
    void phoneMatchesRejectsNonE164Phone() {
        StreamObserver<PhoneMatchesResponse> observer = observer();

        adapter.phoneMatches(PhoneMatchesRequest.newBuilder()
            .setProfileId(profileId.toString())
            .setPhoneE164("3001234567")
            .build(), observer);

        assertEquals(Status.INVALID_ARGUMENT.getCode(), errorOf(observer).getStatus().getCode());
        verifyNoInteractions(service);
    }

    @Test
    void phoneMatchesRejectsNonUuidProfileId() {
        StreamObserver<PhoneMatchesResponse> observer = observer();

        adapter.phoneMatches(PhoneMatchesRequest.newBuilder()
            .setProfileId("not-a-uuid")
            .setPhoneE164("+573001234567")
            .build(), observer);

        assertEquals(Status.INVALID_ARGUMENT.getCode(), errorOf(observer).getStatus().getCode());
        verifyNoInteractions(service);
    }

    @Test
    void updatePasswordRejectsWeakPassword() {
        StreamObserver<UpdatePasswordResponse> observer = observer();

        adapter.updatePassword(UpdatePasswordRequest.newBuilder()
            .setProfileId(profileId.toString())
            .setNewPassword("weak")
            .build(), observer);

        assertEquals(Status.INVALID_ARGUMENT.getCode(), errorOf(observer).getStatus().getCode());
        verifyNoInteractions(service);
    }

    @Test
    void updateEmailMaps409ToAlreadyExists() {
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use"))
            .when(service).updateEmail(profileId, "taken@example.com");
        StreamObserver<UpdateEmailResponse> observer = observer();

        adapter.updateEmail(UpdateEmailRequest.newBuilder()
            .setProfileId(profileId.toString())
            .setNewEmail("taken@example.com")
            .build(), observer);

        assertEquals(Status.ALREADY_EXISTS.getCode(), errorOf(observer).getStatus().getCode());
    }

    @Test
    void updatePhoneCompletesOnSuccess() {
        StreamObserver<UpdatePhoneResponse> observer = observer();

        adapter.updatePhone(UpdatePhoneRequest.newBuilder()
            .setProfileId(profileId.toString())
            .setNewPhoneE164("+573007654321")
            .build(), observer);

        verify(service).updatePhone(profileId, "+573007654321");
        verify(observer).onCompleted();
    }
}
