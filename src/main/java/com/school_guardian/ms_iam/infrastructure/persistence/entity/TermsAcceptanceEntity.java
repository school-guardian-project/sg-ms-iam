package com.school_guardian.ms_iam.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "TermsAcceptance", schema = "Iam")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TermsAcceptanceEntity {
    @Id
    private UUID id;

    @Column(name = "ProfileId", nullable = false)
    private UUID profileId;

    @Column(name = "StudentProfileId")
    private UUID studentProfileId;

    @Column(name = "TermsVersion", nullable = false, length = 20)
    private String termsVersion;

    @Column(name = "AcceptedAt", nullable = false)
    private Instant acceptedAt;

    @Column(name = "IpAddress", length = 50)
    private String ipAddress;

    @Column(name = "Channel", nullable = false, length = 20)
    private String channel;
}
