package com.school_guardian.ms_iam.infrastructure.grpc;

import com.school_guardian.ms_iam.domain.port.out.SchoolDirectory;
import com.school_guardian.ms_iam.infrastructure.grpc.proto.GetAdminSchoolRequest;
import com.school_guardian.ms_iam.infrastructure.grpc.proto.LinkAdminSchoolRequest;
import com.school_guardian.ms_iam.infrastructure.grpc.proto.SchoolManagementServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Cliente gRPC de ms-school-management.
 *
 * <p>El canal se construye con {@code usePlaintext}: los servicios se comunican
 * por la red interna de Docker, sin TLS, igual que el resto del stack. Si en el
 * futuro hay mTLS hay que cambiar a NettyChannelBuilder con el trust manager.
 *
 * <p>Los timeouts son cortos y deliberados: este dato solo mejora el token
 * (claim schoolId), nunca debe hacer lento un login. Ante cualquier fallo se
 * devuelve null y el login continua.
 */
@Component
public class SchoolManagementGrpcClient implements SchoolDirectory, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(SchoolManagementGrpcClient.class);

    private static final long CALL_TIMEOUT_SECONDS = 2;

    private final ManagedChannel channel;
    private final SchoolManagementServiceGrpc.SchoolManagementServiceBlockingStub stub;

    public SchoolManagementGrpcClient(
        @Value("${grpc.school-management.host:ms-school-management}") String host,
        @Value("${grpc.school-management.port:8080}") int port
    ) {
        this.channel = ManagedChannelBuilder.forAddress(host, port)
            .usePlaintext()
            .build();
        this.stub = SchoolManagementServiceGrpc.newBlockingStub(channel);
        log.info("gRPC client toward ms-school-management at {}:{}", host, port);
    }

    @Override
    public AdminSchool findAdminSchool(UUID profileId) {
        try {
            var request = GetAdminSchoolRequest.newBuilder()
                .setProfileId(profileId.toString())
                .build();

            var response = stub
                .withDeadlineAfter(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .getAdminSchool(request);

            if (!response.getFound()) {
                log.debug("No admin-school link for profile {}", profileId);
                return null;
            }

            return new AdminSchool(
                UUID.fromString(response.getId()),
                response.getName(),
                UUID.fromString(response.getCityId())
            );
        } catch (Exception e) {
            // Degradacion: el login no depende de esto. Se registra y se sigue.
            log.warn("Could not resolve school for profile {} via gRPC: {}", profileId, e.toString());
            return null;
        }
    }

    @Override
    public boolean linkAdminSchool(UUID profileId, UUID schoolId) {
        try {
            var request = LinkAdminSchoolRequest.newBuilder()
                .setProfileId(profileId.toString())
                .setSchoolId(schoolId.toString())
                .build();

            var response = stub
                .withDeadlineAfter(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .linkAdminSchool(request);

            if (!response.getLinked()) {
                log.warn("ms-school-management did not link profile {} to school {}", profileId, schoolId);
                return false;
            }

            log.info("Profile {} linked to school {} ({})", profileId, schoolId, response.getSchoolName());
            return true;
        } catch (Exception e) {
            log.warn("Could not link profile {} to school {} via gRPC: {}", profileId, schoolId, e.toString());
            return false;
        }
    }

    @Override
    public void close() {
        channel.shutdown();
        try {
            if (!channel.awaitTermination(2, TimeUnit.SECONDS)) {
                channel.shutdownNow();
            }
        } catch (InterruptedException e) {
            channel.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}