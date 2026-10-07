package com.school_guardian.ms_iam.infrastructure.web;

import com.school_guardian.ms_iam.application.dto.UpdateEmailRequestDto;
import com.school_guardian.ms_iam.application.dto.UpdatePhoneRequestDto;
import com.school_guardian.ms_iam.application.dto.UpdatePasswordRequestDto;
import com.school_guardian.ms_iam.domain.model.PhoneLookup;
import com.school_guardian.ms_iam.domain.model.ProfileLookup;
import com.school_guardian.ms_iam.domain.port.in.FindProfileByEmailUseCase;
import com.school_guardian.ms_iam.domain.port.in.FindProfileByPhoneUseCase;
import com.school_guardian.ms_iam.domain.port.in.MatchProfilePhoneUseCase;
import com.school_guardian.ms_iam.domain.port.in.UpdateProfileEmailUseCase;
import com.school_guardian.ms_iam.domain.port.in.UpdateProfilePhoneUseCase;
import com.school_guardian.ms_iam.domain.port.in.UpdateProfilePasswordUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/profiles")
@Validated
@Tag(name = "Internal profiles", description = "Service-to-service endpoints (X-Internal-Api-Key required)")
@RequiredArgsConstructor
public class ProfileController {

    private final FindProfileByEmailUseCase findProfileByEmailUseCase;
    private final FindProfileByPhoneUseCase findProfileByPhoneUseCase;
    private final UpdateProfilePasswordUseCase updateProfilePasswordUseCase;
    private final UpdateProfileEmailUseCase updateProfileEmailUseCase;
    private final UpdateProfilePhoneUseCase updateProfilePhoneUseCase;
    private final MatchProfilePhoneUseCase matchProfilePhoneUseCase;

    @GetMapping("/by-email")
    @Operation(summary = "Resolve an active profile by login email")
    public ResponseEntity<ProfileLookup> findByEmail(@RequestParam @NotBlank String email) {
        return ResponseEntity.ok(findProfileByEmailUseCase.execute(email));
    }

    @GetMapping("/by-phone")
    @Operation(summary = "Resolve an active profile by phone (E.164 or national format)")
    public ResponseEntity<PhoneLookup> findByPhone(@RequestParam @NotBlank String phone) {
        return ResponseEntity.ok(findProfileByPhoneUseCase.execute(phone));
    }

    @PutMapping("/{profileId}/password")
    @Operation(summary = "Replace a profile password (hashed with the IAM password encoder)")
    public ResponseEntity<Void> updatePassword(
        @PathVariable UUID profileId,
        @Valid @RequestBody UpdatePasswordRequestDto request
    ) {
        updateProfilePasswordUseCase.execute(
            new UpdateProfilePasswordUseCase.UpdatePassword(profileId, request.password()));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{profileId}/email")
    @Operation(summary = "Replace the login email of a profile (stored on its Person)")
    public ResponseEntity<Void> updateEmail(
        @PathVariable UUID profileId,
        @Valid @RequestBody UpdateEmailRequestDto request
    ) {
        updateProfileEmailUseCase.execute(
            new UpdateProfileEmailUseCase.UpdateEmail(profileId, request.email()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{profileId}/phone/matches")
    @Operation(summary = "Tell whether the given phone is the one stored for the profile (the number is never returned)")
    public ResponseEntity<java.util.Map<String, Boolean>> phoneMatches(
        @PathVariable UUID profileId,
        @Valid @RequestBody UpdatePhoneRequestDto request
    ) {
        boolean matches = matchProfilePhoneUseCase.execute(
            new MatchProfilePhoneUseCase.MatchPhone(profileId, request.phone()));
        return ResponseEntity.ok(java.util.Map.of("matches", matches));
    }

    @PutMapping("/{profileId}/phone")
    @Operation(summary = "Replace the phone of a profile (stored on its Person)")
    public ResponseEntity<Void> updatePhone(
        @PathVariable UUID profileId,
        @Valid @RequestBody UpdatePhoneRequestDto request
    ) {
        updateProfilePhoneUseCase.execute(
            new UpdateProfilePhoneUseCase.UpdatePhone(profileId, request.phone()));
        return ResponseEntity.noContent().build();
    }
}