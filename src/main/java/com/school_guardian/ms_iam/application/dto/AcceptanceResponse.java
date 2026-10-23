package com.school_guardian.ms_iam.application.dto;

import java.time.Instant;
import java.util.UUID;

public record AcceptanceResponse(
    UUID id,
    UUID profileId,
    UUID studentProfileId,
    String termsVersion,
    Instant acceptedAt,
    String channel
) {}
