package com.sentinel.gateway.ratelimit;

import com.sentinel.common.enums.AlgorithmType;
import com.sentinel.limiter.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class RateLimiterFactoryTest {

    @Autowired
    private RateLimiterFactory rateLimiterFactory;

    @Test
    void testGetTokenBucketRateLimiter() {
        RateLimiter rateLimiter = rateLimiterFactory.getRateLimiter(AlgorithmType.TOKEN_BUCKET);
        
        assertNotNull(rateLimiter);
        assertTrue(rateLimiter instanceof TokenBucketRateLimiter);
    }

    @Test
    void testGetSlidingWindowLogRateLimiter() {
        RateLimiter rateLimiter = rateLimiterFactory.getRateLimiter(AlgorithmType.SLIDING_WINDOW_LOG);
        
        assertNotNull(rateLimiter);
        assertTrue(rateLimiter instanceof SlidingWindowLogRateLimiter);
    }

    @Test
    void testGetFixedWindowCounterRateLimiter() {
        RateLimiter rateLimiter = rateLimiterFactory.getRateLimiter(AlgorithmType.FIXED_WINDOW_COUNTER);
        
        assertNotNull(rateLimiter);
        assertTrue(rateLimiter instanceof FixedWindowCounterRateLimiter);
    }

    @Test
    void testGetSlidingWindowCounterRateLimiter() {
        RateLimiter rateLimiter = rateLimiterFactory.getRateLimiter(AlgorithmType.SLIDING_WINDOW_COUNTER);
        
        assertNotNull(rateLimiter);
        assertTrue(rateLimiter instanceof SlidingWindowCounterRateLimiter);
    }

    @Test
    void testGetRateLimiterWithInvalidAlgorithm() {
        assertThrows(IllegalArgumentException.class, () -> {
            rateLimiterFactory.getRateLimiter(null);
        });
    }
}