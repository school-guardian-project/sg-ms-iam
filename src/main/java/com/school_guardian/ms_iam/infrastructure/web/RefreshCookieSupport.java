package com.school_guardian.ms_iam.infrastructure.web;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

import java.time.Duration;
import java.time.Instant;

/**
 * Builds and clears the {@code refresh_token} cookie shared by the auth endpoints.
 *
 * <p><b>Design choice: stateless static utility</b> rather than an injected Spring bean.
 * The class holds no collaborators and no configuration (cookie name, path and flags are
 * fixed by the API contract), so a bean would only add indirection. Static calls keep the
 * controllers free of extra constructor dependencies while the behaviour stays byte-for-byte
 * identical to the previous inline code.
 *
 * <p>Contract: {@code HttpOnly}, {@code Secure}, {@code SameSite=Strict} on the
 * set-cookie, path {@code /api/v1/auth} so both {@code /refresh} and {@code /logout}
 * receive it (logout revokes the refresh-token family), {@code Max-Age} derived from
 * the expiry the use case reported so the cookie never outlives the refresh token it
 * carries.
 */
public final class RefreshCookieSupport {

    static final String COOKIE_NAME = "refresh_token";
    static final String COOKIE_PATH = "/api/v1/auth";

    /**
     * Path used before logout needed the cookie. Expiring it on every write avoids
     * a browser keeping both paths and sending the stale one first (longest path
     * wins in the Cookie header), which would look like refresh-token reuse.
     */
    private static final String LEGACY_COOKIE_PATH = "/api/v1/auth/refresh";

    private RefreshCookieSupport() {
    }

    /**
     * Adds a refresh cookie carrying {@code refreshToken}, living until {@code expiresAt}.
     */
    public static void attachRefreshCookie(HttpServletResponse response,
                                           String refreshToken,
                                           Instant expiresAt) {
        expireLegacyCookie(response);

        Cookie cookie = new Cookie(COOKIE_NAME, refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath(COOKIE_PATH);
        cookie.setMaxAge(secondsUntil(expiresAt));
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    /**
     * Adds an empty, already-expired refresh cookie so the browser drops it.
     * Note: this cookie intentionally carries no {@code SameSite} attribute, matching the
     * original logout behaviour.
     */
    public static void clearRefreshCookie(HttpServletResponse response) {
        expireLegacyCookie(response);

        Cookie clearCookie = new Cookie(COOKIE_NAME, "");
        clearCookie.setHttpOnly(true);
        clearCookie.setSecure(true);
        clearCookie.setPath(COOKIE_PATH);
        clearCookie.setMaxAge(0);
        response.addCookie(clearCookie);
    }

    private static void expireLegacyCookie(HttpServletResponse response) {
        Cookie legacy = new Cookie(COOKIE_NAME, "");
        legacy.setHttpOnly(true);
        legacy.setSecure(true);
        legacy.setPath(LEGACY_COOKIE_PATH);
        legacy.setMaxAge(0);
        response.addCookie(legacy);
    }

    /**
     * Remaining lifetime of the refresh cookie, taken from the expiry the use case
     * reported so the cookie never outlives the refresh token it carries.
     */
    private static int secondsUntil(Instant expiresAt) {
        long seconds = Duration.between(Instant.now(), expiresAt).getSeconds();
        return (int) Math.max(seconds, 0);
    }
}
