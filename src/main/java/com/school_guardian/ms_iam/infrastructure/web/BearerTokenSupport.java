package com.school_guardian.ms_iam.infrastructure.web;

/**
 * Manual {@code Authorization: Bearer <token>} parsing shared by the auth controllers.
 *
 * <p><b>Design choice: stateless static utility</b> (no collaborators, no configuration),
 * mirroring {@link RefreshCookieSupport}. Keeping one implementation guarantees that every
 * endpoint reads the header exactly the same way: a missing header, or one that does not
 * start with {@code "Bearer "}, yields {@code null} and the caller answers {@code 401}.
 */
public final class BearerTokenSupport {

    private BearerTokenSupport() {
    }

    /**
     * @return the token after the {@code "Bearer "} prefix, or {@code null} when the header
     * is absent or not a Bearer credential.
     */
    public static String extract(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
