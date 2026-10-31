package com.sentinel.gateway.security;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationFilter implements GatewayFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        
        // Skip authentication for health check and actuator endpoints
        if (path.startsWith("/actuator") || path.startsWith("/health")) {
            return chain.filter(exchange);
        }

        // Never trust a client-supplied identity header
        exchange = exchange.mutate()
                .request(r -> r.headers(h -> h.remove("X-API-Key")))
                .build();

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        if (!jwtUtil.validateToken(token)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // Add the API key to the request headers for downstream services
        String apiKey = jwtUtil.extractApiKey(token);
        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(r -> r.headers(h -> h.set("X-API-Key", apiKey)))
                .build();

        return chain.filter(mutatedExchange);
    }
}