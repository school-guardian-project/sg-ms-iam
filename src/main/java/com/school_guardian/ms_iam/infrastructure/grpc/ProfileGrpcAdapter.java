package com.school_guardian.ms_iam.infrastructure.grpc;

import com.school_guardian.ms_iam.application.usecase.ProfileDirectoryService;
import com.school_guardian.ms_iam.infrastructure.grpc.contract.GetDriverNameRequest;
import com.school_guardian.ms_iam.infrastructure.grpc.contract.GetDriverNameResponse;
import com.school_guardian.ms_iam.infrastructure.grpc.contract.ProfileExistsRequest;
import com.school_guardian.ms_iam.infrastructure.grpc.contract.ProfileExistsResponse;
import com.school_guardian.ms_iam.infrastructure.grpc.contract.ProfileServiceGrpc;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Inbound gRPC adapter (hexagonal driving adapter) for the usermanagement.v1.
 * ProfileService contract consumed by ms-fleet: profile existence and the name of
 * the person behind a profile. It only validates the request declaratively,
 * delegates to the application use case and translates HTTP-style failures into
 * gRPC status codes.
 */
@Component
@RequiredArgsConstructor
public class ProfileGrpcAdapter extends ProfileServiceGrpc.ProfileServiceImplBase {

    private final ProfileDirectoryService service;

    @Override
    public void profileExists(ProfileExistsRequest request, StreamObserver<ProfileExistsResponse> observer) {
        GrpcResponseSupport.respond(observer, () -> {
            UUID profileId = GrpcResponseSupport.requireUuid(request.getProfileId(), "profile_id");
            return ProfileExistsResponse.newBuilder()
                .setExists(service.exists(profileId))
                .build();
        });
    }

    @Override
    public void getDriverName(GetDriverNameRequest request, StreamObserver<GetDriverNameResponse> observer) {
        GrpcResponseSupport.respond(observer, () -> {
            UUID profileId = GrpcResponseSupport.requireUuid(request.getProfileId(), "profile_id");
            return GetDriverNameResponse.newBuilder()
                .setName(service.name(profileId))
                .build();
        });
    }
}
