package com.school_guardian.ms_iam.infrastructure.web;

import com.school_guardian.ms_iam.infrastructure.security.RsaKeyProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Publishes the RS256 public key as a JSON Web Key Set (RFC 7517) so the API
 * gateway can verify access tokens without a shared secret.
 */
@RestController
@RequiredArgsConstructor
public class JwksController {

    private final RsaKeyProvider keyProvider;

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        return Map.of("keys", List.of(keyProvider.publicJwk()));
    }
}
