package com.school_guardian.ms_iam.application.dto;

import java.util.UUID;

public class AuthenticationData {
    public UUID profileId;
    public UUID personId;
    public String email;
    public String passwordHash;
    public Byte roleId;
    public String status;
    public UUID campusId;
    public UUID schoolId;
}