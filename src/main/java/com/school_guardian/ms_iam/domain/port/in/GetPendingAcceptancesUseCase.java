package com.school_guardian.ms_iam.domain.port.in;

import java.util.List;
import java.util.UUID;

public interface GetPendingAcceptancesUseCase {
    PendingAcceptances execute(GetPendingAcceptances command);

    record GetPendingAcceptances(String accessToken) {}

    record PendingAcceptances(
        String termsVersion,
        boolean ownMissing,
        List<MinorInfo> minors
    ) {}

    record MinorInfo(UUID profileId, String name) {}
}