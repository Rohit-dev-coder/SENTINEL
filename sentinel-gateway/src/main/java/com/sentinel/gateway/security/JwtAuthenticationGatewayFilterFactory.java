package com.sentinel.gateway.security;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthenticationGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public JwtAuthenticationGatewayFilterFactory(JwtAuthenticationFilter jwtAuthenticationFilter) {
        super(Object.class);
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Override
    public GatewayFilter apply(Object config) {
        return jwtAuthenticationFilter;
    }
}