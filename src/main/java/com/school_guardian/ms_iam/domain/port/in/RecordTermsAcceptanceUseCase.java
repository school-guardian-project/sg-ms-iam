package com.school_guardian.ms_iam.domain.port.in;

import java.util.List;
import java.util.UUID;

public interface RecordTermsAcceptanceUseCase {
    List<UUID> execute(RecordTermsAcceptance command);

    record RecordTermsAcceptance(
        String accessToken,
        String termsVersion,
        List<UUID> studentProfileIds,
        String ipAddress,
        String channel
    ) {}
}
