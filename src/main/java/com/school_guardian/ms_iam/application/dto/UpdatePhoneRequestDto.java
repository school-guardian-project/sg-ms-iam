package com.school_guardian.ms_iam.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePhoneRequestDto(
        @NotBlank @Size(max = 20) String phone
) {
}