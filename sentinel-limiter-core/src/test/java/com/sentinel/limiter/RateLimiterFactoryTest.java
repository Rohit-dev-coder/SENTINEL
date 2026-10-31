package com.sentinel.limiter;

import com.sentinel.common.enums.AlgorithmType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RateLimiterFactoryTest {

    private static RateLimiter limiterNamed(String name) {
        return new RateLimiter() {
            @Override
            public RateLimitResult allowRequest(String key, int maxRequests, int windowSeconds) {
                return new RateLimitResult(true, maxRequests, 0L);
            }

            @Override
            public String toString() {
                return name;
            }
        };
    }

    @Test
    void acceptsAnyRateLimiterImplementationPerAlgorithm() {
        RateLimiter bucket = limiterNamed("bucket");
        RateLimiter log = limiterNamed("log");
        RateLimiter fixed = limiterNamed("fixed");
        RateLimiter counter = limiterNamed("counter");

        RateLimiterFactory factory = new RateLimiterFactory(bucket, log, fixed, counter);

        assertSame(bucket, factory.getRateLimiter(AlgorithmType.TOKEN_BUCKET));
        assertSame(log, factory.getRateLimiter(AlgorithmType.SLIDING_WINDOW_LOG));
        assertSame(fixed, factory.getRateLimiter(AlgorithmType.FIXED_WINDOW_COUNTER));
        assertSame(counter, factory.getRateLimiter(AlgorithmType.SLIDING_WINDOW_COUNTER));
    }

    @Test
    void rejectsNullAlgorithm() {
        RateLimiter any = limiterNamed("any");
        RateLimiterFactory factory = new RateLimiterFactory(any, any, any, any);

        assertThrows(IllegalArgumentException.class, () -> factory.getRateLimiter(null));
    }
}
