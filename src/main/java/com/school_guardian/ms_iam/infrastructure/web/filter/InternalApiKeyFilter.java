package com.school_guardian.ms_iam.infrastructure.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * Service-to-service authentication for the internal /api/profiles/** endpoints.
 * Fails closed: if no key is configured every request is rejected.
 */
@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Internal-Api-Key";
    public static final String ROLE = "ROLE_INTERNAL_SERVICE";
    private static final String PROTECTED_PREFIX = "/api/profiles";

    private final byte[] expectedKey;

    public InternalApiKeyFilter(@Value("${app.internal.api-key:}") String apiKey) {
        this.expectedKey = apiKey == null ? new byte[0] : apiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        return !(path.equals(PROTECTED_PREFIX) || path.startsWith(PROTECTED_PREFIX + "/"));
    }

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String provided = request.getHeader(HEADER);

        if (expectedKey.length == 0 || provided == null
            || !MessageDigest.isEqual(expectedKey, provided.getBytes(StandardCharsets.UTF_8))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        var auth = new UsernamePasswordAuthenticationToken(
            "internal-service", null, List.of(new SimpleGrantedAuthority(ROLE)));
        SecurityContextHolder.getContext().setAuthentication(auth);

        filterChain.doFilter(request, response);
    }
}