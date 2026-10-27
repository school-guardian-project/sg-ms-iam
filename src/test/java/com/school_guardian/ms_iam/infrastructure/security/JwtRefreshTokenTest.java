package com.school_guardian.ms_iam.infrastructure.security;

import org.junit.jupiter.api.Test;

import java.security.KeyPairGenerator;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtRefreshTokenTest {
    @Test
    void consecutiveRefreshTokensAreUniqueAndRemainVerifiable() throws Exception {
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        var pair = generator.generateKeyPair();
        var keys = mock(RsaKeyProvider.class);
        when(keys.getPrivateKey()).thenReturn(pair.getPrivate());
        when(keys.getPublicKey()).thenReturn(pair.getPublic());
        when(keys.getKeyId()).thenReturn("test-key");
        var provider = new JwtTokenProvider(keys, "school-guardian-iam", "school-guardian-api", 60, 7);
        var profile = UUID.randomUUID();
        var person = UUID.randomUUID();

        var first = provider.generatedRefreshToken(profile, person, "test@example.test", (byte) 1,
            "Admin", "123", null, null);
        var second = provider.generatedRefreshToken(profile, person, "test@example.test", (byte) 1,
            "Admin", "123", null, null);

        assertNotEquals(first, second);
        assertEquals(profile, provider.parseRefreshToken(first).profileId());
        assertEquals(profile, provider.parseRefreshToken(second).profileId());
        assertThrows(io.jsonwebtoken.JwtException.class, () -> provider.parseAccessToken(first));
    }
}
