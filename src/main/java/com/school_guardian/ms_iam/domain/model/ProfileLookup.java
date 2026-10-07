package com.school_guardian.ms_iam.domain.model;

import java.util.UUID;

public record ProfileLookup(UUID profileId, String email) {
}