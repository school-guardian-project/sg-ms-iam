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

    @Column(name = "PasswordHash", nullable = false, length = 100)
    private String passwordHash;

    /**
     * Sede del perfil. Solo aplica a student/driver/parent: un admin pertenece a
     * un colegio, no a una sede, asi que para el rol admin queda null y la
     * relacion vive en School.SchoolAdmin.
     *
     * <p>La columna ya existia en la base de datos pero la entidad no la mapeaba,
     * por lo que la sede se perdia en cada lectura y el claim campusId del token
     * salia siempre nulo.
     */
    @Column(name = "CampuseId")
    private UUID campusId;

    @Column(name = "RoleId", nullable = false)
    private Byte roleId;

    @Column(name = "Status", nullable = false, length = 20)
    private String status;
}
