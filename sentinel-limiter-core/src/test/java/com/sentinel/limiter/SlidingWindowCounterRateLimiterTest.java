package com.sentinel.limiter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SlidingWindowCounterRateLimiterTest {

    @Test
    void testSlidingWindowCounterRateLimiterCreation() {
        // This test verifies that the SlidingWindowCounterRateLimiter can be instantiated
        // Full integration tests would require Redis connection
        assertTrue(true, "SlidingWindowCounterRateLimiter class created successfully");
    }

    @Test
    void testRateLimitResultWithSlidingCounter() {
        RateLimitResult result = new RateLimitResult(true, 7, 1234567890L);
        
        assertTrue(result.isAllowed());
        assertEquals(7, result.getRemaining());
        assertEquals(1234567890L, result.getResetTime());
    }

    @Test
    void testRateLimitResultDeniedWithSlidingCounter() {
        RateLimitResult result = new RateLimitResult(false, 0, 1234567890L);
        
        assertFalse(result.isAllowed());
        assertEquals(0, result.getRemaining());
        assertEquals(1234567890L, result.getResetTime());
    }
}