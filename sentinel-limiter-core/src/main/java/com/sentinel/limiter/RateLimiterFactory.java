package com.sentinel.limiter;

import com.sentinel.common.enums.AlgorithmType;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Looks up the limiter implementation for an {@link AlgorithmType}.
 */
public class RateLimiterFactory {

    private final Map<AlgorithmType, RateLimiter> rateLimiters;

    /** Builds a factory from any implementation per algorithm, for example in-memory limiters in tests. */
    public RateLimiterFactory(
            RateLimiter tokenBucketRateLimiter,
            RateLimiter slidingWindowLogRateLimiter,
            RateLimiter fixedWindowCounterRateLimiter,
            RateLimiter slidingWindowCounterRateLimiter) {
        
        this.rateLimiters = new HashMap<>();
        this.rateLimiters.put(AlgorithmType.TOKEN_BUCKET, tokenBucketRateLimiter);
        this.rateLimiters.put(AlgorithmType.SLIDING_WINDOW_LOG, slidingWindowLogRateLimiter);
        this.rateLimiters.put(AlgorithmType.FIXED_WINDOW_COUNTER, fixedWindowCounterRateLimiter);
        this.rateLimiters.put(AlgorithmType.SLIDING_WINDOW_COUNTER, slidingWindowCounterRateLimiter);
    }

    /** Builds all four limiters on top of one Redis template. */
    public RateLimiterFactory(StringRedisTemplate redisTemplate) {
        this(new TokenBucketRateLimiter(redisTemplate),
             new SlidingWindowLogRateLimiter(redisTemplate),
             new FixedWindowCounterRateLimiter(redisTemplate),
             new SlidingWindowCounterRateLimiter(redisTemplate));
    }

    public RateLimiter getRateLimiter(AlgorithmType algorithmType) {
        RateLimiter rateLimiter = rateLimiters.get(algorithmType);
        if (rateLimiter == null) {
            throw new IllegalArgumentException("Unsupported algorithm type: " + algorithmType);
        }
        return rateLimiter;
    }
}