package com.school_guardian.ms_iam.infrastructure.grpc;

import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * Boots the internal gRPC server (plain h2c, no TLS: the traffic never leaves
 * the Docker network) on a dedicated port, alongside the HTTP edge on :8080.
 * Disable with GRPC_SERVER_ENABLED=false for local HTTP-only runs.
 */
@Component
@Slf4j
public class GrpcServerLifecycle implements SmartLifecycle {

    private final InternalProfileGrpcAdapter internalProfileAdapter;
    private final String internalApiKey;
    private final int port;
    private final boolean enabled;

    private Server server;
    private volatile boolean running;

    public GrpcServerLifecycle(
        InternalProfileGrpcAdapter internalProfileAdapter,
        @Value("${app.internal.api-key:}") String internalApiKey,
        @Value("${grpc.server.port:5001}") int port,
        @Value("${grpc.server.enabled:true}") boolean enabled) {
        this.internalProfileAdapter = internalProfileAdapter;
        this.internalApiKey = internalApiKey;
        this.port = port;
        this.enabled = enabled;
    }

    @Override
    public void start() {
        if (!enabled) {
            log.info("Internal gRPC server disabled by configuration");
            running = true;
            return;
        }
        try {
            server = NettyServerBuilder.forAddress(new InetSocketAddress(port))
                .addService(ServerInterceptors.intercept(internalProfileAdapter,
                    new InternalApiKeyInterceptor(internalApiKey)))
                .build()
                .start();
            running = true;
            log.info("Internal gRPC server listening on port {}", port);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to start internal gRPC server on port " + port, ex);
        }
    }

    @Override
    public void stop() {
        if (server != null) {
            server.shutdown();
        }
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
