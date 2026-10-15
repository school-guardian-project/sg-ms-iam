package com.school_guardian.ms_iam.application.dto;

import java.util.List;
import java.util.UUID;

public class AuthenticationData {
    public UUID profileId;
    public UUID personId;
    public String email;
    public String passwordHash;
    public Byte roleId;
    public String roleName;
    public String identificationNumber;
    public String status;
    public UUID campusId;
    public UUID schoolId;
    public List<String> permissions = List.of();
}