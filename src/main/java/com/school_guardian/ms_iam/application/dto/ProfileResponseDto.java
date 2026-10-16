package com.school_guardian.ms_iam.application.dto;

import java.util.List;
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
        String identificationNumber,
        String status,
        String cityName,
        List<String> permissions
) {
}
