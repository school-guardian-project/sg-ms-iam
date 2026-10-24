package com.school_guardian.ms_iam.infrastructure.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
public class RsaKeyProvider {

    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final String keyId;

    public RsaKeyProvider(
        @Value("${app.jwt.private-key:}") String privateKeyPem,
        @Value("${app.jwt.public-key:}") String publicKeyPem,
        @Value("${app.jwt.key-id:sg-iam}") String keyId
    ) {
        this.keyId = keyId;
        if (hasText(privateKeyPem) && hasText(publicKeyPem)) {
            this.privateKey = parsePrivateKey(normalizePem(privateKeyPem));
            this.publicKey = parsePublicKey(normalizePem(publicKeyPem));
            log.info("JWT RS256 key pair loaded from configuration (kid={})", keyId);
        } else {
            throw new IllegalStateException(
                String.format(
                    "Critical security failure: RSA key pair not configured. "
                        + "Set app.jwt.private-key and app.jwt.public-key to ensure stable "
                        + "tokens and prevent security regressions in production (kid=%s).",
                    keyId
                )
            );
        }
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public String getKeyId() {
        return keyId;
    }

    public Map<String, Object> publicJwk() {
        RSAPublicKey rsa = (RSAPublicKey) publicKey;
        Map<String, Object> jwk = new LinkedHashMap<>();
        jwk.put("kty", "RSA");
        jwk.put("use", "sig");
        jwk.put("alg", "RS256");
        jwk.put("kid", keyId);
        jwk.put("n", base64Url(rsa.getModulus()));
        jwk.put("e", base64Url(rsa.getPublicExponent()));
        return jwk;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String normalizePem(String pem) {
        if (pem.contains("\n")) {
            return pem.replace("\n", "\n");
        }
        return pem;
    }

    private static PrivateKey parsePrivateKey(String pem) {
        try {
            byte[] der = decodePem(pem, "PRIVATE KEY");
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to parse app.jwt.private-key as a PKCS#8 RSA private key", e);
        }
    }

    private static PublicKey parsePublicKey(String pem) {
        try {
            byte[] der = decodePem(pem, "PUBLIC KEY");
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to parse app.jwt.public-key as an X.509 RSA public key", e);
        }
    }

    private static byte[] decodePem(String pem, String type) {
        String base64 = pem
            .replace("-----BEGIN " + type + "-----", "")
            .replace("-----END " + type + "-----", "")
            .replaceAll("\s", "");
        return Base64.getDecoder().decode(base64);
    }

    private static KeyPair generateRsaKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA algorithm not available in this JVM", e);
        }
    }

    private static String base64Url(BigInteger value) {
        byte[] bytes = value.toByteArray();
        if (bytes.length > 1 && bytes[0] == 0) {
            byte[] trimmed = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, trimmed, 0, trimmed.length);
            bytes = trimmed;
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
