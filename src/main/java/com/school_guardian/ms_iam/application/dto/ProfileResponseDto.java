package com.school_guardian.ms_iam.application.dto;

import java.util.UUID;

public record ProfileResponseDto(
        UUID profileId,
        UUID personId,
        String email,
        Byte roleId,
        String roleName,
        UUID campusId,
        String campusName,
        UUID schoolId,
        String schoolName,
        String name,
        String lastName,
        String status
) {
}
