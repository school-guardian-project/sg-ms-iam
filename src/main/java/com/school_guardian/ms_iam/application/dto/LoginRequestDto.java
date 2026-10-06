package com.school_guardian.ms_iam.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LoginRequestDto(
    @NotBlank @Email @Size(max = 100) String email,
    @NotBlank @Size(max = 128) String password
) {}