package com.school_guardian.ms_iam.infrastructure.web.filter;

import com.school_guardian.ms_iam.domain.port.out.TokenDenyList;
import com.school_guardian.ms_iam.domain.port.out.TokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenProvider tokenProvider;
    private final TokenDenyList tokenDenyList;

    /**
     * Rutas donde el token viaja en el header pero NO es un access token.
     *
     * <p>El movil no usa la cookie HttpOnly de la web: manda el refresh token en
     * {@code Authorization: Bearer}. Si este filtro lo interpretara como access
     * token, parseAccessToken falla y la peticion muere con 401 antes de llegar
     * al controlador. Por eso /refresh queda fuera del filtro y es el controlador
     * quien valida el refresh token.
     */
    private static final Set<String> REFRESH_ROUTES = Set.of("/api/v1/auth/refresh");

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        if (REFRESH_ROUTES.contains(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        try {
            var claims = tokenProvider.parseAccessToken(token);

            if (tokenProvider.isTokenExpired(token)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Access token expired");
                return;
            }

            if (tokenDenyList.contains(claims.jti())) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Token revoked");
                return;
            }

            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + claims.roleId()));
            var auth = new UsernamePasswordAuthenticationToken(claims.profileId(), null, authorities);
            auth.setDetails(claims);
            SecurityContextHolder.getContext().setAuthentication(auth);

        } catch (Exception e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid access token");
            return;
        }

        filterChain.doFilter(request, response);
    }
}