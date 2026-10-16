package com.school_guardian.ms_iam.application.usecase;

import com.school_guardian.ms_iam.infrastructure.persistence.repository.ProfileJpaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.UUID;

@Service
public class InternalProfileService {
    public record Lookup(UUID profileId, String email) {}
    private final ProfileJpaRepository profiles;
    private final PasswordEncoder passwords;
    private final String countryCode;

    public InternalProfileService(ProfileJpaRepository profiles, PasswordEncoder passwords,
                                  @Value("${app.phone.default-country-code:57}") String countryCode) {
        this.profiles = profiles;
        this.passwords = passwords;
        this.countryCode = countryCode;
    }

    public Lookup findByEmail(String email) {
        return profiles.findActiveByEmail(normalizeEmail(email)).stream().findFirst()
            .map(row -> new Lookup(UUID.fromString(row[0].toString()), row[1].toString()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Active profile not found"));
    }

    @Transactional
    public void updatePassword(UUID id, String password) {
        var profile = profiles.findById(id)
            .filter(p -> "Active".equals(p.getStatus()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Active profile not found"));
        profile.setPasswordHash(passwords.encode(password));
        profiles.save(profile);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.SERIALIZABLE)
    public void updateEmail(UUID id, String email) {
        String normalized = normalizeEmail(email);
        boolean occupied = profiles.findActiveByEmail(normalized).stream()
            .anyMatch(row -> !id.toString().equalsIgnoreCase(row[0].toString()));
        if (occupied) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        }
        requireUpdated(profiles.updateEmail(id, normalized));
    }

    public boolean phoneMatches(UUID id, String phone) {
        long national = nationalPhone(phone);
        return profiles.findActivePhone(id).map(stored -> stored == national)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Active profile not found"));
    }

    @Transactional
    public void updatePhone(UUID id, String phone) {
        requireUpdated(profiles.updatePhone(id, nationalPhone(phone)));
    }

    private long nationalPhone(String phone) {
        if (phone == null || !phone.matches("^\\+[1-9]\\d{7,14}$") ||
            !phone.startsWith("+" + countryCode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported phone country or format");
        }
        String national = phone.substring(countryCode.length() + 1);
        if (national.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid phone");
        }
        return Long.parseLong(national);
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static void requireUpdated(int rows) {
        if (rows != 1) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Active profile not found");
        }
    }
}
