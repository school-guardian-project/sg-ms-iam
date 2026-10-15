package com.school_guardian.ms_iam.infrastructure.grpc;

import com.school_guardian.ms_iam.application.usecase.InternalProfileService;
import com.school_guardian.ms_iam.infrastructure.grpc.contract.FindProfileByEmailRequest;
import com.school_guardian.ms_iam.infrastructure.grpc.contract.InternalProfileServiceGrpc;
import com.school_guardian.ms_iam.infrastructure.grpc.contract.PhoneMatchesRequest;
import com.school_guardian.ms_iam.infrastructure.grpc.contract.PhoneMatchesResponse;
import com.school_guardian.ms_iam.infrastructure.grpc.contract.ProfileLookup;
import com.school_guardian.ms_iam.infrastructure.grpc.contract.UpdateEmailRequest;
import com.school_guardian.ms_iam.infrastructure.grpc.contract.UpdateEmailResponse;
import com.school_guardian.ms_iam.infrastructure.grpc.contract.UpdatePasswordRequest;
import com.school_guardian.ms_iam.infrastructure.grpc.contract.UpdatePasswordResponse;
import com.school_guardian.ms_iam.infrastructure.grpc.contract.UpdatePhoneRequest;
import com.school_guardian.ms_iam.infrastructure.grpc.contract.UpdatePhoneResponse;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Inbound gRPC adapter (hexagonal driving adapter) for the internal profile
 * contract iam.v1.InternalProfileService. It only validates the request
 * declaratively, delegates to the application use case and translates
 * HTTP-style failures into gRPC status codes.
 */
@Component
@RequiredArgsConstructor
public class InternalProfileGrpcAdapter extends InternalProfileServiceGrpc.InternalProfileServiceImplBase {

    private static final String PHONE_E164_REGEX = "^\\+[1-9]\\d{7,14}$";
    private static final String PASSWORD_REGEX =
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&.\\-_])[A-Za-z\\d@$!%*?&.\\-_]{8,}$";
    private static final int EMAIL_MAX_LENGTH = 100;

    private final InternalProfileService service;

    @Override
    public void findProfileByEmail(FindProfileByEmailRequest request, StreamObserver<ProfileLookup> observer) {
        GrpcResponseSupport.respond(observer, () -> {
            requireEmail(request.getEmail());
            InternalProfileService.Lookup lookup = service.findByEmail(request.getEmail());
            return ProfileLookup.newBuilder()
                .setProfileId(lookup.profileId().toString())
                .setEmail(lookup.email())
                .build();
        });
    }

    @Override
    public void phoneMatches(PhoneMatchesRequest request, StreamObserver<PhoneMatchesResponse> observer) {
        GrpcResponseSupport.respond(observer, () -> {
            UUID profileId = GrpcResponseSupport.requireUuid(request.getProfileId(), "profile_id");
            requirePhone(request.getPhoneE164());
            return PhoneMatchesResponse.newBuilder()
                .setMatches(service.phoneMatches(profileId, request.getPhoneE164()))
                .build();
        });
    }

    @Override
    public void updatePassword(UpdatePasswordRequest request, StreamObserver<UpdatePasswordResponse> observer) {
        GrpcResponseSupport.respond(observer, () -> {
            UUID profileId = GrpcResponseSupport.requireUuid(request.getProfileId(), "profile_id");
            requirePassword(request.getNewPassword());
            service.updatePassword(profileId, request.getNewPassword());
            return UpdatePasswordResponse.getDefaultInstance();
        });
    }

    @Override
    public void updateEmail(UpdateEmailRequest request, StreamObserver<UpdateEmailResponse> observer) {
        GrpcResponseSupport.respond(observer, () -> {
            UUID profileId = GrpcResponseSupport.requireUuid(request.getProfileId(), "profile_id");
            requireEmail(request.getNewEmail());
            service.updateEmail(profileId, request.getNewEmail());
            return UpdateEmailResponse.getDefaultInstance();
        });
    }

    @Override
    public void updatePhone(UpdatePhoneRequest request, StreamObserver<UpdatePhoneResponse> observer) {
        GrpcResponseSupport.respond(observer, () -> {
            UUID profileId = GrpcResponseSupport.requireUuid(request.getProfileId(), "profile_id");
            requirePhone(request.getNewPhoneE164());
            service.updatePhone(profileId, request.getNewPhoneE164());
            return UpdatePhoneResponse.getDefaultInstance();
        });
    }

    private static void requireEmail(String email) {
        if (email == null || email.isBlank() || email.length() > EMAIL_MAX_LENGTH
            || !email.contains("@") || email.contains(" ")) {
            throw new IllegalArgumentException("email must be a valid address");
        }
    }

    private static void requirePhone(String phone) {
        if (phone == null || !phone.matches(PHONE_E164_REGEX)) {
            throw new IllegalArgumentException("phone must be E.164 (e.g. +573001234567)");
        }
    }

    private static void requirePassword(String password) {
        if (password == null || !password.matches(PASSWORD_REGEX)) {
            throw new IllegalArgumentException(
                "password must have 8+ chars with upper, lower, digit and one of @$!%*?&.-_");
        }
    }
}
