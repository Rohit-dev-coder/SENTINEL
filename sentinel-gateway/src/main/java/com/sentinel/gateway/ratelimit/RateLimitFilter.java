package com.sentinel.gateway.ratelimit;

import com.sentinel.common.enums.AlgorithmType;
import com.sentinel.gateway.config.TenantConfigClient;
import com.sentinel.gateway.config.TenantLimitConfig;
import com.sentinel.limiter.RateLimitResult;
import com.sentinel.limiter.RateLimiterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class RateLimitFilter implements GatewayFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final RateLimiterFactory rateLimiterFactory;
    private final TenantConfigClient tenantConfigClient;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public RateLimitFilter(RateLimiterFactory rateLimiterFactory, TenantConfigClient tenantConfigClient) {
        this.rateLimiterFactory = rateLimiterFactory;
        this.tenantConfigClient = tenantConfigClient;
    }

    /** The limit actually applied to a request. */
    record Limit(AlgorithmType algorithm, int maxRequests, int windowSeconds) {}

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Set by JwtAuthenticationFilter from the verified token
        String apiKey = exchange.getRequest().getHeaders().getFirst("X-API-Key");
        if (apiKey == null) {
            return reject(exchange, HttpStatus.UNAUTHORIZED);
        }
        String path = exchange.getRequest().getPath().value();
        String endpoint = extractEndpoint(exchange.getRequest());

        return tenantConfigClient.getConfig(apiKey)
                .flatMap(config -> {
                    if (!config.isActive()) {
                        return reject(exchange, HttpStatus.FORBIDDEN);
                    }
                    Limit limit = resolveLimit(config, path);
                    // Redis access is blocking, so keep it off the event loop
                    return Mono.fromCallable(() -> rateLimiterFactory.getRateLimiter(limit.algorithm())
                                    .allowRequest(apiKey + ":" + endpoint, limit.maxRequests(), limit.windowSeconds()))
                            .subscribeOn(Schedulers.boundedElastic())
                            .onErrorResume(e -> {
                                // Fail open: a Redis outage should not take the API down
                                log.error("Rate limiter unavailable, allowing request: {}", e.toString());
                                return Mono.just(new RateLimitResult(true, limit.maxRequests(), 0L));
                            })
                            .flatMap(result -> apply(exchange, chain, limit, result));
                })
                // Valid JWT but no such tenant
                .switchIfEmpty(Mono.defer(() -> reject(exchange, HttpStatus.FORBIDDEN)));
    }

    /** First active rule whose pattern matches the path wins; otherwise the tenant's tier defaults apply. */
    Limit resolveLimit(TenantLimitConfig config, String path) {
        for (TenantLimitConfig.Rule rule : config.getRules()) {
            if (rule.getEndpointPattern() != null && pathMatcher.match(rule.getEndpointPattern(), path)) {
                return new Limit(rule.getAlgorithm(), rule.getMaxRequests(), rule.getWindowSeconds());
            }
        }
        return new Limit(AlgorithmType.TOKEN_BUCKET, config.getDefaultMaxRequests(), config.getDefaultWindowSeconds());
    }

    private Mono<Void> apply(ServerWebExchange exchange, GatewayFilterChain chain, Limit limit, RateLimitResult result) {
        var headers = exchange.getResponse().getHeaders();
        headers.set("X-RateLimit-Limit", String.valueOf(limit.maxRequests()));
        headers.set("X-RateLimit-Remaining", String.valueOf(result.isAllowed() ? result.getRemaining() : 0));
        headers.set("X-RateLimit-Reset", String.valueOf(result.getResetTime()));
        if (!result.isAllowed()) {
            return reject(exchange, HttpStatus.TOO_MANY_REQUESTS);
        }
        return chain.filter(exchange);
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }

    private String extractEndpoint(ServerHttpRequest request) {
        return request.getMethod().name() + ":" + request.getPath().value();
    }
}
