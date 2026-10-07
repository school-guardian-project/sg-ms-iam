package com.school_guardian.ms_iam.application.dto;

import java.util.UUID;

public class AuthenticationData {
    public UUID profileId;
    public UUID personId;
    public String email;
    public String passwordHash;
    public Byte roleId;
    public String status;

    /**
     * Sede del perfil, leida de {@code Iam.Profile.CampuseId}. Va null para
     * admins: un admin se relaciona con un colegio, no con una sede.
     */
    public UUID campusId;
}