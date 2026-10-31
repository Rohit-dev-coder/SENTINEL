package com.sentinel.limiter;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;

public class TokenBucketRateLimiter implements RateLimiter {

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<Long> tokenBucketScript;

    public TokenBucketRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.tokenBucketScript = RedisScript.of(getTokenBucketLuaScript(), Long.class);
    }

    @Override
    public RateLimitResult allowRequest(String key, int maxRequests, int windowSeconds) {
        long currentTime = System.currentTimeMillis() / 1000; // Convert to seconds
        long windowStart = currentTime - (currentTime % windowSeconds);
        
        // Redis key format: token_bucket:{key}
        String redisKey = "token_bucket:" + key;
        
        // Execute Lua script
        Long result = redisTemplate.execute(
            tokenBucketScript,
            Collections.singletonList(redisKey),
            String.valueOf(maxRequests),
            String.valueOf(windowSeconds),
            String.valueOf(currentTime)
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

    private String getTokenBucketLuaScript() {
        return """
            local key = KEYS[1]
            local max_tokens = tonumber(ARGV[1])
            local window_seconds = tonumber(ARGV[2])
            local current_time = tonumber(ARGV[3])
            
            -- Get current state
            local data = redis.call('HMGET', key, 'tokens', 'last_refill')
            local tokens = tonumber(data[1] or max_tokens)
            local last_refill = tonumber(data[2] or current_time)
            
            -- Calculate time elapsed since last refill
            local elapsed = current_time - last_refill
            
            -- Refill tokens based on elapsed time; only advance last_refill by the
            -- time actually converted into whole tokens so slow rates do not starve
            if elapsed > 0 then
                local refill_rate = max_tokens / window_seconds
                local new_tokens = math.floor(elapsed * refill_rate)
                if new_tokens > 0 then
                    tokens = math.min(max_tokens, tokens + new_tokens)
                    last_refill = last_refill + (new_tokens / refill_rate)
                end
            end
            
            -- Check if we have enough tokens
            if tokens >= 1 then
                -- Consume one token
                tokens = tokens - 1
                redis.call('HMSET', key, 'tokens', tokens, 'last_refill', last_refill)
                redis.call('EXPIRE', key, window_seconds)
                
                -- Return allowed (1)
                return 1
            else
                -- Update last refill time even if not allowed
                redis.call('HMSET', key, 'tokens', tokens, 'last_refill', last_refill)
                redis.call('EXPIRE', key, window_seconds)
                
                -- Return not allowed (0)
                return 0
            end
            """;
    }
}