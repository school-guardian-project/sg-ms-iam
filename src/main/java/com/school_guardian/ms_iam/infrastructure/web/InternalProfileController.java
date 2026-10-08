package com.school_guardian.ms_iam.infrastructure.web;

import com.school_guardian.ms_iam.application.usecase.InternalProfileService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/profiles")
@Validated
@RequiredArgsConstructor
public class InternalProfileController {
    public record EmailRequest(@NotBlank @Email @Size(max = 100) String email) {}
    public record PhoneRequest(@NotBlank @Pattern(regexp = "^\\+[1-9]\\d{7,14}$") String phone) {}
    public record PasswordRequest(
        @NotBlank @Size(max = 72)
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&.\\-_])[A-Za-z\\d@$!%*?&.\\-_]{8,}$")
        String password) {}
    private final InternalProfileService service;

    @GetMapping("/by-email")
    public InternalProfileService.Lookup findByEmail(@RequestParam @NotBlank @Email String email) {
        return service.findByEmail(email);
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<Void> updatePassword(@PathVariable UUID id, @Valid @RequestBody PasswordRequest body) {
        service.updatePassword(id, body.password());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/email")
    public ResponseEntity<Void> updateEmail(@PathVariable UUID id, @Valid @RequestBody EmailRequest body) {
        service.updateEmail(id, body.email());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/phone/matches")
    public Map<String, Boolean> phoneMatches(@PathVariable UUID id, @Valid @RequestBody PhoneRequest body) {
        return Map.of("matches", service.phoneMatches(id, body.phone()));
    }

    @PutMapping("/{id}/phone")
    public ResponseEntity<Void> updatePhone(@PathVariable UUID id, @Valid @RequestBody PhoneRequest body) {
        service.updatePhone(id, body.phone());
        return ResponseEntity.noContent().build();
    }
}
