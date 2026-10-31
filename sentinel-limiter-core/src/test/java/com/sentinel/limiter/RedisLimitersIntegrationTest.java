package com.sentinel.limiter;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the real limiter classes (and their Lua scripts) against a real Redis. Skipped when Docker
 * is not available.
 */
@Testcontainers(disabledWithoutDocker = true)
class RedisLimitersIntegrationTest {

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    private static LettuceConnectionFactory factory;
    private static StringRedisTemplate template;

    @BeforeAll
    static void connect() {
        factory = new LettuceConnectionFactory(
                new RedisStandaloneConfiguration(REDIS.getHost(), REDIS.getMappedPort(6379)));
        factory.afterPropertiesSet();
        template = new StringRedisTemplate(factory);
        template.afterPropertiesSet();
    }

    @AfterAll
    static void disconnect() {
        if (factory != null) {
            factory.destroy();
        }
    }

    private static String uniqueKey() {
        return "test-" + UUID.randomUUID();
    }

    /** Sends {@code attempts} requests and returns how many were allowed. */
    private static int countAllowed(RateLimiter limiter, String key, int limit, int window, int attempts) {
        int allowed = 0;
        for (int i = 0; i < attempts; i++) {
            if (limiter.allowRequest(key, limit, window).isAllowed()) {
                allowed++;
            }
        }
        return allowed;
    }

    @Test
    void tokenBucketAllowsBurstThenBlocks() {
        assertEquals(5, countAllowed(new TokenBucketRateLimiter(template), uniqueKey(), 5, 60, 12));
    }

    @Test
    void tokenBucketKeepsKeysIndependent() {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(template);
        String a = uniqueKey();
        String b = uniqueKey();
        assertEquals(2, countAllowed(limiter, a, 2, 60, 5));
        assertTrue(limiter.allowRequest(b, 2, 60).isAllowed());
    }

    @Test
    void tokenBucketRefillsAtLowRates() throws InterruptedException {
        // 2 per 4 s => one token every 2 s. Requests arriving every second must still earn tokens.
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(template);
        String key = uniqueKey();
        assertTrue(limiter.allowRequest(key, 2, 4).isAllowed());
        assertTrue(limiter.allowRequest(key, 2, 4).isAllowed());
        assertFalse(limiter.allowRequest(key, 2, 4).isAllowed());
        boolean allowedAgain = false;
        for (int i = 0; i < 6 && !allowedAgain; i++) {
            Thread.sleep(1000);
            allowedAgain = limiter.allowRequest(key, 2, 4).isAllowed();
        }
        assertTrue(allowedAgain, "bucket must refill even when requests keep arriving");
    }

    @Test
    void slidingWindowCounterBlocksAfterLimit() {
        assertEquals(5, countAllowed(new SlidingWindowCounterRateLimiter(template), uniqueKey(), 5, 60, 12));
    }

    @Test
    void fixedWindowCounterBlocksAfterLimit() {
        assertEquals(5, countAllowed(new FixedWindowCounterRateLimiter(template), uniqueKey(), 5, 60, 12));
    }

    @Test
    void slidingWindowLogBlocksAfterLimit() {
        assertEquals(5, countAllowed(new SlidingWindowLogRateLimiter(template), uniqueKey(), 5, 60, 12));
    }
}
