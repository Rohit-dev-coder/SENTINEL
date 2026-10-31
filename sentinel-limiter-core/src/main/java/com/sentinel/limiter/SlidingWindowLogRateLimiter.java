package com.sentinel.limiter;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;

public class SlidingWindowLogRateLimiter implements RateLimiter {

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<Long> slidingWindowLogScript;

    public SlidingWindowLogRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.slidingWindowLogScript = RedisScript.of(getSlidingWindowLogLuaScript(), Long.class);
    }

    @Override
    public RateLimitResult allowRequest(String key, int maxRequests, int windowSeconds) {
        long currentTime = System.currentTimeMillis();
        long windowStart = currentTime - (windowSeconds * 1000L);
        
        // Redis key format: sliding_window:{key}
        String redisKey = "sliding_window:" + key;
        
        // Execute Lua script
        Long result = redisTemplate.execute(
            slidingWindowLogScript,
            Collections.singletonList(redisKey),
            String.valueOf(maxRequests),
            String.valueOf(windowStart),
            String.valueOf(currentTime)
        );
        
        if (result == null) {
            // Fallback: allow request if Redis is unavailable
            return new RateLimitResult(true, maxRequests, currentTime / 1000 + windowSeconds);
        }
        
        // Result format: allowed (1) or not allowed (0)
        boolean allowed = result == 1;
        int remaining = allowed ? maxRequests - 1 : 0;
        
        return new RateLimitResult(allowed, remaining, currentTime / 1000 + windowSeconds);
    }

    private String getSlidingWindowLogLuaScript() {
        return """
            local key = KEYS[1]
            local max_requests = tonumber(ARGV[1])
            local window_start = tonumber(ARGV[2])
            local current_time = tonumber(ARGV[3])
            
            -- Remove timestamps outside the sliding window
            redis.call('ZREMRANGEBYSCORE', key, '-inf', window_start)
            
            -- Count requests in the current window
            local count = redis.call('ZCARD', key)
            
            -- Check if we're under the limit
            if count < max_requests then
                -- Add current request timestamp
                redis.call('ZADD', key, current_time, current_time)
                redis.call('EXPIRE', key, math.ceil(max_requests / 10) + 1)
                
                -- Return allowed (1)
                return 1
            else
                -- Return not allowed (0)
                return 0
            end
            """;
    }
}