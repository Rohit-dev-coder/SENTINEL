package com.sentinel.limiter;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;

public class SlidingWindowCounterRateLimiter implements RateLimiter {

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<Long> slidingWindowCounterScript;

    public SlidingWindowCounterRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.slidingWindowCounterScript = RedisScript.of(getSlidingWindowCounterLuaScript(), Long.class);
    }

    @Override
    public RateLimitResult allowRequest(String key, int maxRequests, int windowSeconds) {
        long currentTime = System.currentTimeMillis();
        long windowStart = currentTime - (windowSeconds * 1000L);
        
        // Redis key format: sliding_counter:{key}
        String redisKey = "sliding_counter:" + key;
        
        // Execute Lua script
        Long result = redisTemplate.execute(
            slidingWindowCounterScript,
            Collections.singletonList(redisKey),
            String.valueOf(maxRequests),
            String.valueOf(windowStart),
            String.valueOf(currentTime),
            String.valueOf(windowSeconds)
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

    private String getSlidingWindowCounterLuaScript() {
        return """
            local key = KEYS[1]
            local max_requests = tonumber(ARGV[1])
            local window_start = tonumber(ARGV[2])
            local current_time = tonumber(ARGV[3])
            local window_seconds = tonumber(ARGV[4])
            
            -- Counters live in a hash: field = 10-second bucket id, value = count
            local oldest_bucket = math.floor(window_start / 10000)
            local counters = redis.call('HGETALL', key)
            local total_count = 0
            for i = 1, #counters, 2 do
                if tonumber(counters[i]) <= oldest_bucket then
                    redis.call('HDEL', key, counters[i])
                else
                    total_count = total_count + tonumber(counters[i + 1])
                end
            end
            
            if total_count < max_requests then
                local bucket = math.floor(current_time / 10000)
                redis.call('HINCRBY', key, bucket, 1)
                redis.call('EXPIRE', key, window_seconds + 10)
                return 1
            else
                return 0
            end
            """;
    }
}