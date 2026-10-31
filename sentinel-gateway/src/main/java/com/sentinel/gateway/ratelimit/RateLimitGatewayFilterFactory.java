package com.sentinel.gateway.ratelimit;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;

@Component
public class RateLimitGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    private final RateLimitFilter rateLimitFilter;

    public RateLimitGatewayFilterFactory(RateLimitFilter rateLimitFilter) {
        super(Object.class);
        this.rateLimitFilter = rateLimitFilter;
    }

    @Override
    public GatewayFilter apply(Object config) {
        return rateLimitFilter;
    }
}