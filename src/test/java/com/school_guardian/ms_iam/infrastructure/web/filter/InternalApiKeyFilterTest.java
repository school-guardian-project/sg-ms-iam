package com.school_guardian.ms_iam.infrastructure.web.filter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;

class InternalApiKeyFilterTest {
    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validKeyAuthenticatesInternalService() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/profiles/by-email");
        request.addHeader("X-Internal-Api-Key", "test-key");
        var response = new MockHttpServletResponse();
        new InternalApiKeyFilter("test-key").doFilter(request, response, (req, res) ->
            assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream().anyMatch(a -> a.getAuthority().equals("ROLE_INTERNAL_SERVICE"))));
        assertEquals(200, response.getStatus());
    }

    @Test
    void missingWrongAndUnconfiguredKeysAreRejected() throws Exception {
        for (String configured : new String[]{"test-key", ""}) {
            for (String supplied : new String[]{"wrong-key", ""}) {
                var request = new MockHttpServletRequest("GET", "/api/profiles/by-email");
                if (!supplied.isEmpty()) request.addHeader("X-Internal-Api-Key", supplied);
                var response = new MockHttpServletResponse();
                new InternalApiKeyFilter(configured).doFilter(request, response,
                    (req, res) -> fail("Rejected request must not reach controller"));
                assertEquals(401, response.getStatus());
            }
        }
    }

    @Test
    void loginIsUnaffected() throws Exception {
        var request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        var response = new MockHttpServletResponse();
        new InternalApiKeyFilter("").doFilter(request, response,
            (req, res) -> ((MockHttpServletResponse) res).setStatus(202));
        assertEquals(202, response.getStatus());
    }
}
