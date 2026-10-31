package com.sentinel.limiter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenBucketRateLimiterTest {

    @Test
    void testRateLimitResult() {
        RateLimitResult result = new RateLimitResult(true, 5, 1234567890L);
        
        assertTrue(result.isAllowed());
        assertEquals(5, result.getRemaining());
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
    void testRateLimitResultToString() {
        RateLimitResult result = new RateLimitResult(true, 10, 1234567890L);
        String toString = result.toString();
        
        assertTrue(toString.contains("allowed=true"));
        assertTrue(toString.contains("remaining=10"));
        assertTrue(toString.contains("resetTime=1234567890"));
    }
}