package com.school_guardian.ms_iam.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.List;
import java.util.UUID;

/**
 * @param channel client that records the acceptance: {@code "web"} or {@code "mobile"}
 *                (case-insensitive). Optional; when absent it is stored as
 *                {@code "unspecified"} instead of guessing.
 */
public record AcceptTermsRequest(
    @NotBlank String termsVersion,
    List<UUID> studentProfileIds,
    @Pattern(regexp = "(?i)web|mobile", message = "channel must be 'web' or 'mobile'") String channel
) {}
