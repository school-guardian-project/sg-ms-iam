package com.school_guardian.ms_iam.application.dto;

import java.util.UUID;

public record UserProfileDto(
    UUID profileId,
    UUID personId,
    String email,
    Byte roleId,
    String roleName,
    UUID campusId,
    /**
     * Colegio del admin. Aditivo: los clientes que ya lo consumen no se rompen y
     * los que no lo conocen simplemente lo ignoran. Va null para roles que no
     * administran un colegio (student, driver, parent).
     */
    UUID schoolId
) {
}
