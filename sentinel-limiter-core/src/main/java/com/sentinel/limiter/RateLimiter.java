package com.sentinel.limiter;

public interface RateLimiter {
    
    /**
     * Check if a request should be allowed based on rate limiting rules
     * 
     * @param key Unique identifier for the rate limit (e.g., tenant:endpoint)
     * @param maxRequests Maximum number of requests allowed
     * @param windowSeconds Time window in seconds
     * @return RateLimitResult containing whether the request is allowed and remaining tokens
     */
    RateLimitResult allowRequest(String key, int maxRequests, int windowSeconds);
}