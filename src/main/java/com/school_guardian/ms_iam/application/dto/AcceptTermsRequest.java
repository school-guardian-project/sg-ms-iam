package com.school_guardian.ms_iam.application.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.UUID;

public record AcceptTermsRequest(
    @NotBlank String termsVersion,
    List<UUID> studentProfileIds
) {}
