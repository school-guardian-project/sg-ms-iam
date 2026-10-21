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
@Table(name = "SessionProfile", schema = "Iam")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SessionProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "ProfileID", nullable = false)
    private UUID profileId;

    @Column(name = "DateStart", nullable = false, columnDefinition = "datetime2")
    private Instant dateStart;

    @Column(name = "DateEnd", nullable = false, columnDefinition = "datetime2")
    private Instant dateEnd;

    @Column(name = "IPAddress", nullable = false, length = 50)
    private String ipAddress;

    @Column(name = "StatusSession", nullable = false, length = 20)
    private String statusSession;
}
