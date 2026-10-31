package com.sentinel.gateway.ratelimit;

import com.sentinel.common.enums.AlgorithmType;
import com.sentinel.common.enums.Tier;
import com.sentinel.gateway.config.TenantLimitConfig;
import com.sentinel.limiter.RateLimitResult;
import com.sentinel.limiter.RateLimiter;
import com.sentinel.limiter.RateLimiterFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitFilterTest {

    @Test
    void testRateLimitResultCreation() {
        RateLimitResult result = new RateLimitResult(true, 10, 1234567890L);
        
        assertTrue(result.isAllowed());
        assertEquals(10, result.getRemaining());
        assertEquals(1234567890L, result.getResetTime());
    }

    @Test
    void testRateLimitResultDenied() {
        RateLimitResult result = new RateLimitResult(false, 0, 1234567890L);
        
        assertFalse(result.isAllowed());
        assertEquals(0, result.getRemaining());
        assertEquals(1234567890L, result.getResetTime());
    }

    @Test
    void testResolveLimitUsesTierDefaultsWhenNoRuleMatches() {
        RateLimitFilter filter = new RateLimitFilter(null, null);
        RateLimitFilter.Limit limit = filter.resolveLimit(TenantLimitConfig.forTier(Tier.FREE), "/api/v1/users/1");

        assertEquals(AlgorithmType.TOKEN_BUCKET, limit.algorithm());
        assertEquals(Tier.FREE.getDefaultMaxRequests(), limit.maxRequests());
        assertEquals(Tier.FREE.getDefaultWindowSeconds(), limit.windowSeconds());
    }

    @Test
    void testResolveLimitPrefersMatchingRule() {
        TenantLimitConfig config = TenantLimitConfig.forTier(Tier.FREE);
        TenantLimitConfig.Rule rule = new TenantLimitConfig.Rule();
        rule.setEndpointPattern("/api/v1/orders/**");
        rule.setAlgorithm(AlgorithmType.FIXED_WINDOW_COUNTER);
        rule.setMaxRequests(5);
        rule.setWindowSeconds(30);
        config.setRules(java.util.List.of(rule));

        RateLimitFilter filter = new RateLimitFilter(null, null);
        RateLimitFilter.Limit limit = filter.resolveLimit(config, "/api/v1/orders/42");

        assertEquals(AlgorithmType.FIXED_WINDOW_COUNTER, limit.algorithm());
        assertEquals(5, limit.maxRequests());
        assertEquals(30, limit.windowSeconds());
    }
}
