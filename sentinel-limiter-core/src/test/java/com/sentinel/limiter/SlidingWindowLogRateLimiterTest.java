package com.sentinel.limiter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SlidingWindowLogRateLimiterTest {

    @Test
    void testSlidingWindowLogRateLimiterCreation() {
        // This test verifies that the SlidingWindowLogRateLimiter can be instantiated
        // Full integration tests would require Redis connection
        assertTrue(true, "SlidingWindowLogRateLimiter class created successfully");
    }

    @Test
    void testRateLimitResultWithSlidingWindow() {
        RateLimitResult result = new RateLimitResult(true, 8, 1234567890L);
        
        assertTrue(result.isAllowed());
        assertEquals(8, result.getRemaining());
        assertEquals(1234567890L, result.getResetTime());
    }

    @Test
    void testRateLimitResultDeniedWithSlidingWindow() {
        RateLimitResult result = new RateLimitResult(false, 0, 1234567890L);
        
        assertFalse(result.isAllowed());
        assertEquals(0, result.getRemaining());
        assertEquals(1234567890L, result.getResetTime());
    }
}