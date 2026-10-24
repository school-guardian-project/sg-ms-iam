package com.school_guardian.ms_iam.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * One row per refresh token issued by IAM.
 *
 * <p>Every login creates one {@link SessionProfileEntity} row (the session) and one
 * RefreshToken row. Each successful refresh revokes the presented token and inserts a
 * new one that points back to it through {@code ReplacedByTokenId}. Reusing a token
 * that was already rotated means the whole session (all tokens sharing
 * {@code SessionProfileId}) has been compromised, so IAM revokes the family.</p>
 *
 * <p>Only the SHA-256 hash of the token is stored; the raw token never touches the
 * database, so a read-only leak of this table cannot be replayed.</p>
 */
@Entity
@Table(name = "RefreshToken", schema = "Iam")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "SessionProfileId", nullable = false)
    private Integer sessionProfileId;

    @Column(name = "TokenHash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "CreatedAt", nullable = false, columnDefinition = "datetime2")
    private Instant createdAt;

    @Column(name = "ExpiresAt", nullable = false, columnDefinition = "datetime2")
    private Instant expiresAt;

    @Column(name = "RevokedAt", columnDefinition = "datetime2")
    private Instant revokedAt;

    @Column(name = "ReplacedByTokenId")
    private Long replacedByTokenId;
}
