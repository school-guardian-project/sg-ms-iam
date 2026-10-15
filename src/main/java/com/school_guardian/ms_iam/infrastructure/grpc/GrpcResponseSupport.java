package com.school_guardian.ms_iam.infrastructure.grpc;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Shared behaviour for the inbound gRPC driving adapters: a uniform
 * request/response wrapper that maps HTTP-style failures onto gRPC status codes,
 * plus the small request validators reused across contracts. Keeps each adapter
 * focused on translating its own contract.
 */
@Slf4j
final class GrpcResponseSupport {

    private GrpcResponseSupport() {
    }

    static <T> void respond(StreamObserver<T> observer, Supplier<T> action) {
        try {
            observer.onNext(action.get());
            observer.onCompleted();
        } catch (ResponseStatusException ex) {
            observer.onError(toGrpcStatus(ex).asRuntimeException());
        } catch (IllegalArgumentException ex) {
            observer.onError(Status.INVALID_ARGUMENT
                .withDescription(ex.getMessage())
                .asRuntimeException());
        } catch (Exception ex) {
            log.error("Unexpected error handling internal gRPC call", ex);
            observer.onError(Status.INTERNAL
                .withDescription("Unexpected error")
                .asRuntimeException());
        }
    }

    static Status toGrpcStatus(ResponseStatusException ex) {
        Status status = switch (ex.getStatusCode().value()) {
            case 404 -> Status.NOT_FOUND;
            case 409 -> Status.ALREADY_EXISTS;
            case 400 -> Status.INVALID_ARGUMENT;
            default -> Status.INTERNAL;
        };
        return status.withDescription(ex.getReason() != null ? ex.getReason() : status.getDescription());
    }

    static UUID requireUuid(String raw, String field) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(field + " must be a UUID");
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(field + " must be a UUID");
        }
    }
}
