package com.sentinel.starter;

import com.sentinel.limiter.RateLimiterFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Registers the Sentinel rate limit filter in Spring MVC (servlet) applications that have Redis configured.
 * Switch it off with {@code sentinel.enabled=false}.
 */
@AutoConfiguration(afterName = "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({StringRedisTemplate.class, OncePerRequestFilter.class})
@ConditionalOnProperty(prefix = "sentinel", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(SentinelProperties.class)
public class SentinelAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RateLimiterFactory sentinelRateLimiterFactory(StringRedisTemplate redisTemplate) {
        return new RateLimiterFactory(redisTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    public LimitResolver sentinelLimitResolver() {
        return new LimitResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    public SentinelRateLimitFilter sentinelRateLimitFilter(SentinelProperties properties,
                                                           RateLimiterFactory limiters,
                                                           LimitResolver resolver) {
        properties.validate();
        SentinelAdminClient adminClient = properties.getAdmin().isEnabled()
                ? new SentinelAdminClient(properties.getAdmin())
                : null;
        return new SentinelRateLimitFilter(properties, limiters, resolver, adminClient);
    }

    @Bean
    public FilterRegistrationBean<SentinelRateLimitFilter> sentinelRateLimitFilterRegistration(
            SentinelRateLimitFilter filter, SentinelProperties properties) {
        FilterRegistrationBean<SentinelRateLimitFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(properties.getFilterOrder());
        registration.addUrlPatterns("/*");
        return registration;
    }
}
