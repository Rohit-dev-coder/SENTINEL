package com.sentinel.gateway.ratelimit;

import com.sentinel.limiter.RateLimiterFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RateLimiterConfig {

    @Bean
    public RateLimiterFactory rateLimiterFactory(StringRedisTemplate redisTemplate) {
        return new RateLimiterFactory(redisTemplate);
    }
}
