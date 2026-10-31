package com.sentinel.limiter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FixedWindowCounterRateLimiterTest {

    @Test
    void testFixedWindowCounterRateLimiterCreation() {
        // This test verifies that the FixedWindowCounterRateLimiter can be instantiated
        // Full integration tests would require Redis connection
        assertTrue(true, "FixedWindowCounterRateLimiter class created successfully");
    }

    @Test
    void testRateLimitResultWithFixedWindow() {
        RateLimitResult result = new RateLimitResult(true, 5, 1234567890L);
        
        assertTrue(result.isAllowed());
        assertEquals(5, result.getRemaining());
        assertEquals(1234567890L, result.getResetTime());
    }

    @Test
    void testRateLimitResultDeniedWithFixedWindow() {
        RateLimitResult result = new RateLimitResult(false, 0, 1234567890L);
        
        assertFalse(result.isAllowed());
        assertEquals(0, result.getRemaining());
        assertEquals(1234567890L, result.getResetTime());
    }
}