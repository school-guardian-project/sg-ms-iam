package com.school_guardian.ms_iam.infrastructure.grpc;

import com.school_guardian.ms_iam.application.usecase.ProfileDirectoryService;
import com.school_guardian.ms_iam.infrastructure.grpc.contract.GetDriverNameRequest;
import com.school_guardian.ms_iam.infrastructure.grpc.contract.GetDriverNameResponse;
import com.school_guardian.ms_iam.infrastructure.grpc.contract.ProfileExistsRequest;
import com.school_guardian.ms_iam.infrastructure.grpc.contract.ProfileExistsResponse;
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

class ProfileGrpcAdapterTest {

    private final ProfileDirectoryService service = mock(ProfileDirectoryService.class);
    private final ProfileGrpcAdapter adapter = new ProfileGrpcAdapter(service);
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
    void profileExistsDelegatesAndReturnsFlag() {
        when(service.exists(profileId)).thenReturn(true);
        StreamObserver<ProfileExistsResponse> observer = observer();

        adapter.profileExists(
            ProfileExistsRequest.newBuilder().setProfileId(profileId.toString()).build(), observer);

        verify(observer).onNext(ProfileExistsResponse.newBuilder().setExists(true).build());
        verify(observer).onCompleted();
    }

    @Test
    void profileExistsRejectsNonUuidProfileId() {
        StreamObserver<ProfileExistsResponse> observer = observer();

        adapter.profileExists(
            ProfileExistsRequest.newBuilder().setProfileId("not-a-uuid").build(), observer);

        assertEquals(Status.INVALID_ARGUMENT.getCode(), errorOf(observer).getStatus().getCode());
        verifyNoInteractions(service);
    }

    @Test
    void getDriverNameDelegatesAndMapsName() {
        when(service.name(profileId)).thenReturn("Ana Pérez");
        StreamObserver<GetDriverNameResponse> observer = observer();

        adapter.getDriverName(
            GetDriverNameRequest.newBuilder().setProfileId(profileId.toString()).build(), observer);

        verify(observer).onNext(GetDriverNameResponse.newBuilder().setName("Ana Pérez").build());
        verify(observer).onCompleted();
    }

    @Test
    void getDriverNameMaps404ToNotFound() {
        when(service.name(profileId))
            .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Active profile not found"));
        StreamObserver<GetDriverNameResponse> observer = observer();

        adapter.getDriverName(
            GetDriverNameRequest.newBuilder().setProfileId(profileId.toString()).build(), observer);

        assertEquals(Status.NOT_FOUND.getCode(), errorOf(observer).getStatus().getCode());
    }
}
