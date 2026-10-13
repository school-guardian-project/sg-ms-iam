package com.school_guardian.ms_iam.infrastructure.grpc;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * gRPC counterpart of the former InternalApiKeyFilter: every call must carry
 * the shared secret in the "x-internal-api-key" metadata header. The key is
 * injected from configuration (environment), never hardcoded.
 */
public class InternalApiKeyInterceptor implements ServerInterceptor {

    public static final Metadata.Key<String> API_KEY_HEADER =
        Metadata.Key.of("x-internal-api-key", Metadata.ASCII_STRING_MARSHALLER);

    private final byte[] expectedKey;

    public InternalApiKeyInterceptor(String apiKey) {
        this.expectedKey = apiKey == null ? new byte[0] : apiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
        ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {

        String provided = headers.get(API_KEY_HEADER);
        if (expectedKey.length == 0 || provided == null
            || !MessageDigest.isEqual(expectedKey, provided.getBytes(StandardCharsets.UTF_8))) {
            call.close(Status.UNAUTHENTICATED.withDescription("Missing or invalid internal API key"),
                new Metadata());
            return new ServerCall.Listener<>() { };
        }
        return next.startCall(call, headers);
    }
}
