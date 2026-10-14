package com.school_guardian.ms_iam.infrastructure.grpc;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.Status;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InternalApiKeyInterceptorTest {

    private final ServerCall<Object, Object> call = mock(ServerCall.class);
    private final ServerCallHandler<Object, Object> next = mock(ServerCallHandler.class);

    private Metadata headersWith(String key) {
        var headers = new Metadata();
        if (key != null) {
            headers.put(InternalApiKeyInterceptor.API_KEY_HEADER, key);
        }
        return headers;
    }

    @Test
    void validKeyReachesTheService() {
        var interceptor = new InternalApiKeyInterceptor("test-key");
        var headers = headersWith("test-key");

        interceptor.interceptCall(call, headers, next);

        verify(next).startCall(call, headers);
        verify(call, never()).close(any(), any());
    }

    @Test
    void wrongKeyIsRejectedUnauthenticated() {
        var interceptor = new InternalApiKeyInterceptor("test-key");

        interceptor.interceptCall(call, headersWith("other-key"), next);

        ArgumentCaptor<Status> status = ArgumentCaptor.forClass(Status.class);
        verify(call).close(status.capture(), any());
        assertEquals(Status.UNAUTHENTICATED.getCode(), status.getValue().getCode());
        verifyNoInteractions(next);
    }

    @Test
    void missingKeyIsRejectedUnauthenticated() {
        var interceptor = new InternalApiKeyInterceptor("test-key");

        interceptor.interceptCall(call, headersWith(null), next);

        ArgumentCaptor<Status> status = ArgumentCaptor.forClass(Status.class);
        verify(call).close(status.capture(), any());
        assertEquals(Status.UNAUTHENTICATED.getCode(), status.getValue().getCode());
        verifyNoInteractions(next);
    }

    @Test
    void unsetServerKeyRejectsEverything() {
        var interceptor = new InternalApiKeyInterceptor("");

        interceptor.interceptCall(call, headersWith("any-key"), next);

        verify(call).close(any(), any());
        verifyNoInteractions(next);
    }
}
