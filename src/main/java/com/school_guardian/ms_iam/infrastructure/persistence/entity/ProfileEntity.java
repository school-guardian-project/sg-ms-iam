package com.school_guardian.ms_iam.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "Profile", schema = "Iam")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProfileEntity {
    @Id
    private UUID id;

    @Column(name = "PersonId", nullable = false)
    private UUID personId;

    @Column(name = "CampuseId")
    private UUID campusId;

    @Column(name = "PasswordHash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "RoleId", nullable = false)
    private Byte roleId;

    @Column(name = "Status", nullable = false, length = 20)
    private String status;
}
