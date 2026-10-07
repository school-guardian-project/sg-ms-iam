package com.school_guardian.ms_iam.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequestDto(
    @NotBlank String currentPassword,
    @NotBlank @Size(min = 8, message = "New password must be at least 8 characters") String newPassword
) {
}
