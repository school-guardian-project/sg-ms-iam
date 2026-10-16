package com.school_guardian.ms_iam.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TermsAcceptance {
    private UUID id;
    private UUID profileId;
    private UUID studentProfileId;
    private String termsVersion;
    private Instant acceptedAt;
    private String ipAddress;
    private String channel;
}
