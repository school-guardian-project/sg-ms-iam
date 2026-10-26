package com.school_guardian.ms_iam.infrastructure.web;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds and clears the {@code refresh_token} cookie shared by the auth endpoints.
 *
 * <p><b>Design choice: stateless static utility</b> rather than an injected Spring bean.
 * The class holds no collaborators and no configuration (cookie name, path and flags are
 * fixed by the API contract), so a bean would only add indirection. Static calls keep the
 * controllers free of extra constructor dependencies while the behaviour stays byte-for-byte
 * identical to the previous inline code.
 *
 * <p>Contract: {@code HttpOnly}, {@code Secure}, {@code SameSite=None} on the
 * set-cookie, path {@code /api/v1/auth/refresh}, {@code Max-Age} derived from the expiry the
 * use case reported so the cookie never outlives the refresh token it carries.
 * SameSite=None is required for cross-origin requests (e.g., Vercel frontend -> duckdns.org API).
 */
public final class RefreshCookieSupport {

    private static final Logger log = LoggerFactory.getLogger(RefreshCookieSupport.class);
    static final String COOKIE_NAME = "refresh_token";
    static final String COOKIE_PATH = "/api/v1/auth/refresh";

    private RefreshCookieSupport() {
    }

    /**
     * Adds a refresh cookie carrying {@code refreshToken}, living until {@code expiresAt}.
     */
    public static void attachRefreshCookie(HttpServletResponse response,
                                           String refreshToken,
                                           Instant expiresAt) {
        Cookie cookie = new Cookie(COOKIE_NAME, refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath(COOKIE_PATH);
        cookie.setMaxAge(secondsUntil(expiresAt));
        cookie.setAttribute("SameSite", "None");
        response.addCookie(cookie);
        
        log.debug("Attached refresh cookie: path={}, maxAge={}, secure={}, httpOnly={}, sameSite=None",
                  COOKIE_PATH, cookie.getMaxAge(), cookie.getSecure(), cookie.isHttpOnly());
    }

    /**
     * Adds an empty, already-expired refresh cookie so the browser drops it.
     * <p>
     * Must include {@code SameSite=None} + {@code Secure} to match the cookie set during
     * login/refresh, otherwise the browser will not delete the cross-origin cookie.
     */
    public static void clearRefreshCookie(HttpServletResponse response) {
        Cookie clearCookie = new Cookie(COOKIE_NAME, "");
        clearCookie.setHttpOnly(true);
        clearCookie.setSecure(true);
        clearCookie.setPath(COOKIE_PATH);
        clearCookie.setMaxAge(0);
        clearCookie.setAttribute("SameSite", "None");
        response.addCookie(clearCookie);
        
        log.debug("Cleared refresh cookie: path={}, maxAge=0, secure={}, httpOnly={}, sameSite=None",
                  COOKIE_PATH, clearCookie.getSecure(), clearCookie.isHttpOnly());
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
