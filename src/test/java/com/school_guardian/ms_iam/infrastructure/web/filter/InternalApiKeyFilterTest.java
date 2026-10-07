package com.school_guardian.ms_iam.infrastructure.web.filter;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class InternalApiKeyFilterTest {

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletResponse call(String configuredKey, String path, String header, FilterChain chain) throws Exception {
        var request = new MockHttpServletRequest("GET", path);
        if (header != null) request.addHeader(InternalApiKeyFilter.HEADER, header);
        var response = new MockHttpServletResponse();
        new InternalApiKeyFilter(configuredKey).doFilter(request, response, chain);
        return response;
    }

    @Test
    void correctKeyIsAccepted() throws Exception {
        var chain = mock(FilterChain.class);
        var response = call("secret", "/api/profiles/by-email", "secret", chain);

        assertEquals(200, response.getStatus());
        verify(chain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals(InternalApiKeyFilter.ROLE)));
    }

    @Test
    void wrongOrMissingKeyIsRejected() throws Exception {
        var chain = mock(FilterChain.class);

        assertEquals(401, call("secret", "/api/profiles/x/password", "wrong", chain).getStatus());
        assertEquals(401, call("secret", "/api/profiles/x/password", null, chain).getStatus());
        verify(chain, never()).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void unconfiguredKeyRejectsEverything() throws Exception {
        var chain = mock(FilterChain.class);

        assertEquals(401, call("", "/api/profiles/by-email", "", chain).getStatus());
        verify(chain, never()).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void otherPathsAreNotFiltered() throws Exception {
        var chain = mock(FilterChain.class);

        assertEquals(200, call("secret", "/api/v1/auth/login", null, chain).getStatus());
        verify(chain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}