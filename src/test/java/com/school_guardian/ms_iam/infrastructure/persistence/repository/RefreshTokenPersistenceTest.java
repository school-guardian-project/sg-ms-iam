package com.school_guardian.ms_iam.infrastructure.persistence.repository;

import com.school_guardian.ms_iam.infrastructure.persistence.entity.RefreshTokenEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RefreshTokenPersistenceTest {
    private final RefreshTokenJpaRepository tokens = mock(RefreshTokenJpaRepository.class);
    private final AuthenticationRepositoryImpl repository =
        new AuthenticationRepositoryImpl(mock(ProfileJpaRepository.class), tokens);

    @Test
    void savesTheInitialLoginToken() {
        var token = new RefreshTokenEntity();
        repository.saveRefreshToken(token);
        verify(tokens).save(token);
        assertNull(token.getRevokedAt());
    }

    @Test
    void rotationRevokesTheOldTokenAndLinksItsReplacement() {
        var oldToken = new RefreshTokenEntity();
        oldToken.setId(1L);
        var replacement = new RefreshTokenEntity();
        when(tokens.save(replacement)).thenAnswer(invocation -> {
            replacement.setId(2L);
            return replacement;
        });

        repository.saveRefreshToken(replacement, oldToken);

        assertNotNull(oldToken.getRevokedAt());
        assertEquals(2L, oldToken.getReplacedByTokenId());
        verify(tokens).save(oldToken);
        assertNull(replacement.getRevokedAt());
    }
}
