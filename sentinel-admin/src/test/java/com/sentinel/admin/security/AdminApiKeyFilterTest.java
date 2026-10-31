package com.sentinel.admin.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class AdminApiKeyFilterTest {

    private final FilterChain chain = mock(FilterChain.class);

    private MockHttpServletResponse run(String configuredKey, String method, String uri, String headerValue)
            throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        if (headerValue != null) {
            request.addHeader(AdminApiKeyFilter.HEADER, headerValue);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        new AdminApiKeyFilter(configuredKey).doFilter(request, response, chain);
        return response;
    }

    @Test
    void allowsRequestWithCorrectKey() throws Exception {
        MockHttpServletResponse response = run("secret", "GET", "/api/v1/admin/tenants", "secret");

        assertEquals(200, response.getStatus());
        verify(chain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsMissingKey() throws Exception {
        MockHttpServletResponse response = run("secret", "GET", "/api/v1/admin/tenants", null);

        assertEquals(401, response.getStatus());
        verify(chain, never()).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsWrongKey() throws Exception {
        assertEquals(401, run("secret", "GET", "/api/v1/admin/tenants", "nope").getStatus());
    }

    @Test
    void failsClosedWhenNoKeyConfigured() throws Exception {
        assertEquals(401, run("", "GET", "/api/v1/admin/tenants", "").getStatus());
    }

    @Test
    void healthAndPreflightBypassAuth() throws Exception {
        assertEquals(200, run("secret", "GET", "/actuator/health", null).getStatus());
        assertEquals(200, run("secret", "OPTIONS", "/api/v1/admin/tenants", null).getStatus());
    }

    @Test
    void metricsRequireAuth() throws Exception {
        assertEquals(401, run("secret", "GET", "/actuator/metrics", null).getStatus());
    }
}
