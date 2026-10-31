package com.sentinel.limiter;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;

public class FixedWindowCounterRateLimiter implements RateLimiter {

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<Long> fixedWindowCounterScript;

    public FixedWindowCounterRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.fixedWindowCounterScript = RedisScript.of(getFixedWindowCounterLuaScript(), Long.class);
    }

    @Override
    public RateLimitResult allowRequest(String key, int maxRequests, int windowSeconds) {
        long currentTime = System.currentTimeMillis() / 1000; // Convert to seconds
        long windowStart = currentTime - (currentTime % windowSeconds);
        
        // Redis key format: fixed_window:{key}:{windowStart}
        String redisKey = "fixed_window:" + key + ":" + windowStart;
        
        // Execute Lua script
        Long result = redisTemplate.execute(
            fixedWindowCounterScript,
            Collections.singletonList(redisKey),
            String.valueOf(maxRequests),
            String.valueOf(windowSeconds)
        );
        
        if (result == null) {
            // Fallback: allow request if Redis is unavailable
            return new RateLimitResult(true, maxRequests, windowStart + windowSeconds);
        }
        
        // Result format: allowed (1) or not allowed (0)
        boolean allowed = result == 1;
        int remaining = allowed ? maxRequests - 1 : 0;
        
        return new RateLimitResult(allowed, remaining, windowStart + windowSeconds);
    }

    private String getFixedWindowCounterLuaScript() {
        return """
            local key = KEYS[1]
            local max_requests = tonumber(ARGV[1])
            local window_seconds = tonumber(ARGV[2])
            
            -- Get current count
            local count = tonumber(redis.call('GET', key) or '0')
            
            -- Check if we're under the limit
            if count < max_requests then
                -- Increment counter
                redis.call('INCR', key)
                redis.call('EXPIRE', key, window_seconds)
                
                -- Return allowed (1)
                return 1
            else
                -- Return not allowed (0)
                return 0
            end
            """;
    }
}