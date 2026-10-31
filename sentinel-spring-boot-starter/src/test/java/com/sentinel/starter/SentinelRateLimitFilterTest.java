package com.sentinel.starter;

import com.sentinel.common.enums.AlgorithmType;
import com.sentinel.limiter.RateLimitResult;
import com.sentinel.limiter.RateLimiter;
import com.sentinel.limiter.RateLimiterFactory;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SentinelRateLimitFilterTest {

    private final SentinelProperties properties = new SentinelProperties();
    private final RateLimiterFactory factory = mock(RateLimiterFactory.class);
    private final RateLimiter limiter = mock(RateLimiter.class);
    private final FilterChain chain = mock(FilterChain.class);

    @BeforeEach
    void setUp() {
        when(factory.getRateLimiter(any(AlgorithmType.class))).thenReturn(limiter);
    }

    private SentinelRateLimitFilter standalone() {
        return new SentinelRateLimitFilter(properties, factory, new LimitResolver(), null);
    }

    private MockHttpServletResponse run(SentinelRateLimitFilter filter, MockHttpServletRequest request) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        return response;
    }

    private static MockHttpServletRequest get(String uri, String clientId) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setRemoteAddr("10.0.0.7");
        if (clientId != null) {
            request.addHeader("X-API-Key", clientId);
        }
        return request;
    }

    private static long nowSeconds() {
        return System.currentTimeMillis() / 1000;
    }

    @Test
    void allowedRequestPassesThroughWithRateLimitHeaders() throws Exception {
        when(limiter.allowRequest(anyString(), anyInt(), anyInt())).thenReturn(new RateLimitResult(true, 99, nowSeconds() + 60));

        MockHttpServletResponse response = run(standalone(), get("/api/orders", "client-1"));

        verify(chain).doFilter(any(), any());
        assertEquals(200, response.getStatus());
        assertEquals("100", response.getHeader("X-RateLimit-Limit"));
        assertEquals("99", response.getHeader("X-RateLimit-Remaining"));
        assertNotNull(response.getHeader("X-RateLimit-Reset"));
    }

    @Test
    void deniedRequestGets429WithRetryAfter() throws Exception {
        when(limiter.allowRequest(anyString(), anyInt(), anyInt())).thenReturn(new RateLimitResult(false, 0, nowSeconds() + 30));

        MockHttpServletResponse response = run(standalone(), get("/api/orders", "client-1"));

        verify(chain, never()).doFilter(any(), any());
        assertEquals(429, response.getStatus());
        assertEquals("0", response.getHeader("X-RateLimit-Remaining"));
        long retryAfter = Long.parseLong(response.getHeader("Retry-After"));
        assertTrue(retryAfter >= 1 && retryAfter <= 30, "Retry-After was " + retryAfter);
        assertTrue(response.getContentAsString().contains("rate_limit_exceeded"));
    }

    @Test
    void limiterKeyIsClientMethodAndPath() throws Exception {
        when(limiter.allowRequest(anyString(), anyInt(), anyInt())).thenReturn(new RateLimitResult(true, 1, nowSeconds() + 60));

        run(standalone(), get("/api/orders", "client-1"));

        verify(limiter).allowRequest(eq("client-1:GET:/api/orders"), eq(100), eq(60));
    }

    @Test
    void propertyRuleOverridesDefault() throws Exception {
        SentinelProperties.Rule rule = new SentinelProperties.Rule();
        rule.setPathPattern("/api/orders/**");
        rule.setAlgorithm(AlgorithmType.FIXED_WINDOW_COUNTER);
        rule.setMaxRequests(5);
        rule.setWindowSeconds(30);
        properties.setRules(List.of(rule));
        when(factory.getRateLimiter(AlgorithmType.FIXED_WINDOW_COUNTER)).thenReturn(limiter);
        when(limiter.allowRequest(anyString(), anyInt(), anyInt())).thenReturn(new RateLimitResult(true, 4, nowSeconds() + 30));

        run(standalone(), get("/api/orders/42", "client-1"));

        verify(limiter).allowRequest(anyString(), eq(5), eq(30));
    }

    @Test
    void fallsBackToClientIpWhenHeaderMissing() throws Exception {
        when(limiter.allowRequest(anyString(), anyInt(), anyInt())).thenReturn(new RateLimitResult(true, 1, nowSeconds() + 60));

        run(standalone(), get("/api/orders", null));

        verify(limiter).allowRequest(eq("ip-10.0.0.7:GET:/api/orders"), anyInt(), anyInt());
    }

    @Test
    void rejectsMissingClientIdWhenFallbackIsReject() throws Exception {
        properties.setIdentityFallback(SentinelProperties.IdentityFallback.REJECT);

        MockHttpServletResponse response = run(standalone(), get("/api/orders", null));

        assertEquals(401, response.getStatus());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void excludedPathsAndPreflightAreNotLimited() throws Exception {
        run(standalone(), get("/actuator/health", "client-1"));
        run(standalone(), new MockHttpServletRequest("OPTIONS", "/api/orders"));

        verify(limiter, never()).allowRequest(anyString(), anyInt(), anyInt());
        verify(chain, org.mockito.Mockito.times(2)).doFilter(any(), any());
    }

    @Test
    void redisFailureAllowsRequestByDefault() throws Exception {
        when(limiter.allowRequest(anyString(), anyInt(), anyInt())).thenThrow(new IllegalStateException("redis down"));

        MockHttpServletResponse response = run(standalone(), get("/api/orders", "client-1"));

        verify(chain).doFilter(any(), any());
        assertEquals(200, response.getStatus());
    }

    @Test
    void redisFailureRejectsWhenFailClosed() throws Exception {
        properties.setFailOpen(false);
        when(limiter.allowRequest(anyString(), anyInt(), anyInt())).thenThrow(new IllegalStateException("redis down"));

        MockHttpServletResponse response = run(standalone(), get("/api/orders", "client-1"));

        verify(chain, never()).doFilter(any(), any());
        assertEquals(503, response.getStatus());
    }

    // ---- connected mode (limits come from the Sentinel admin service) ----

    private AdminTenantConfig tenant(boolean active) {
        AdminTenantConfig config = new AdminTenantConfig();
        config.setActive(active);
        config.setTier("PREMIUM");
        config.setDefaultMaxRequests(1000);
        config.setDefaultWindowSeconds(60);
        return config;
    }

    @Test
    void connectedModeUsesTierDefaultsFromAdmin() throws Exception {
        SentinelAdminClient admin = mock(SentinelAdminClient.class);
        when(admin.lookup("sk_abc")).thenReturn(Optional.of(tenant(true)));
        when(limiter.allowRequest(anyString(), anyInt(), anyInt())).thenReturn(new RateLimitResult(true, 999, nowSeconds() + 60));

        run(new SentinelRateLimitFilter(properties, factory, new LimitResolver(), admin), get("/api/orders", "sk_abc"));

        verify(limiter).allowRequest(anyString(), eq(1000), eq(60));
    }

    @Test
    void connectedModeAppliesAdminRules() throws Exception {
        AdminTenantConfig config = tenant(true);
        AdminTenantConfig.Rule rule = new AdminTenantConfig.Rule();
        rule.setEndpointPattern("/api/orders/**");
        rule.setAlgorithm(AlgorithmType.TOKEN_BUCKET);
        rule.setMaxRequests(3);
        rule.setWindowSeconds(10);
        config.setRules(List.of(rule));
        SentinelAdminClient admin = mock(SentinelAdminClient.class);
        when(admin.lookup("sk_abc")).thenReturn(Optional.of(config));
        when(limiter.allowRequest(anyString(), anyInt(), anyInt())).thenReturn(new RateLimitResult(true, 2, nowSeconds() + 10));

        run(new SentinelRateLimitFilter(properties, factory, new LimitResolver(), admin), get("/api/orders/9", "sk_abc"));

        verify(limiter).allowRequest(anyString(), eq(3), eq(10));
    }

    @Test
    void connectedModeRejectsUnknownAndInactiveTenants() throws Exception {
        SentinelAdminClient admin = mock(SentinelAdminClient.class);
        when(admin.lookup("unknown")).thenReturn(Optional.empty());
        when(admin.lookup("inactive")).thenReturn(Optional.of(tenant(false)));
        SentinelRateLimitFilter filter = new SentinelRateLimitFilter(properties, factory, new LimitResolver(), admin);

        assertEquals(403, run(filter, get("/api/orders", "unknown")).getStatus());
        assertEquals(403, run(filter, get("/api/orders", "inactive")).getStatus());
        verify(limiter, never()).allowRequest(anyString(), anyInt(), anyInt());
    }

    @Test
    void connectedModeRequiresAClientKey() throws Exception {
        SentinelAdminClient admin = mock(SentinelAdminClient.class);
        SentinelRateLimitFilter filter = new SentinelRateLimitFilter(properties, factory, new LimitResolver(), admin);

        assertEquals(401, run(filter, get("/api/orders", null)).getStatus());
    }
}
