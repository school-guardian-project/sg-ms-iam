package com.school_guardian.ms_iam.domain.model;

import com.school_guardian.ms_iam.shared.Status;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Profile {
    private UUID id;
    private UUID personId;
    private String passwordHash;
    private Role role;
    private Status status;

    /** Sede del perfil. Null para admins, que se relacionan con un colegio. */
    private UUID campusId;
}
