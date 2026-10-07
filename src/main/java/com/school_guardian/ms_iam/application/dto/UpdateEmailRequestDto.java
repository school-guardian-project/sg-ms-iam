package com.school_guardian.ms_iam.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateEmailRequestDto(
        @NotBlank @Email @Size(max = 50) String email
) {
}