package com.sentinel.gateway.ratelimit;

import com.sentinel.common.enums.AlgorithmType;
import com.sentinel.limiter.RateLimitResult;
import com.sentinel.limiter.RateLimiter;
import com.sentinel.limiter.RateLimiterFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIfSystemProperty(named = "docker.tests.enabled", matches = "true")
class RateLimitIntegrationTest {

    @Autowired
    private RateLimiterFactory rateLimiterFactory;

    @Test
    void testPerTenantRateLimiting() {
        // Test rate limiting for different tenants
        RateLimiter rateLimiter = rateLimiterFactory.getRateLimiter(AlgorithmType.TOKEN_BUCKET);
        
        String tenant1Key = "tenant1:/api/v1/users";
        String tenant2Key = "tenant2:/api/v1/users";
        
        // Both tenants should be allowed initially
        RateLimitResult result1 = rateLimiter.allowRequest(tenant1Key, 10, 60);
        RateLimitResult result2 = rateLimiter.allowRequest(tenant2Key, 10, 60);
        
        assertTrue(result1.isAllowed());
        assertTrue(result2.isAllowed());
        
        // Clean up
        // In real integration test, we would clean up Redis keys
    }

    @Test
    void testPerEndpointRateLimiting() {
        // Test rate limiting for different endpoints
        RateLimiter rateLimiter = rateLimiterFactory.getRateLimiter(AlgorithmType.TOKEN_BUCKET);
        
        String endpoint1Key = "tenant1:/api/v1/users";
        String endpoint2Key = "tenant1:/api/v1/products";
        
        // Both endpoints should be allowed initially
        RateLimitResult result1 = rateLimiter.allowRequest(endpoint1Key, 5, 60);
        RateLimitResult result2 = rateLimiter.allowRequest(endpoint2Key, 5, 60);
        
        assertTrue(result1.isAllowed());
        assertTrue(result2.isAllowed());
        
        // Clean up
        // In real integration test, we would clean up Redis keys
    }

    @Test
    void testRateLimitExceeded() {
        // Test rate limit enforcement
        RateLimiter rateLimiter = rateLimiterFactory.getRateLimiter(AlgorithmType.TOKEN_BUCKET);
        
        String key = "tenant1:/api/v1/users";
        int maxRequests = 2;
        
        // First two requests should be allowed
        RateLimitResult result1 = rateLimiter.allowRequest(key, maxRequests, 60);
        RateLimitResult result2 = rateLimiter.allowRequest(key, maxRequests, 60);
        
        assertTrue(result1.isAllowed());
        assertTrue(result2.isAllowed());
        
        // Third request should be denied
        RateLimitResult result3 = rateLimiter.allowRequest(key, maxRequests, 60);
        assertFalse(result3.isAllowed());
        
        // Clean up
        // In real integration test, we would clean up Redis keys
    }
}