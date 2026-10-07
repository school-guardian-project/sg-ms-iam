package com.school_guardian.ms_iam.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePasswordRequestDto(
        @NotBlank @Size(min = 8, max = 128) String password
) {
}