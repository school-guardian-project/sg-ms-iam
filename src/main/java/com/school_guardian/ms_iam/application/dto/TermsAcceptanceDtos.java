package com.school_guardian.ms_iam.application.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class TermsAcceptanceDtos {

    public record AcceptTermsRequest(
        @NotBlank String termsVersion,
        List<UUID> studentProfileIds
    ) {}

    public record AcceptanceResponse(
        UUID id,
        UUID profileId,
        UUID studentProfileId,
        String termsVersion,
        Instant acceptedAt,
        String channel
    ) {}
}
