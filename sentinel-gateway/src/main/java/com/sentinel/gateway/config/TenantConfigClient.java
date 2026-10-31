package com.sentinel.gateway.config;

import com.sentinel.common.enums.Tier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves a tenant's tier and rate-limit rules from the admin service, with a short in-memory cache.
 * Emits an empty Mono when the tenant is unknown. If the admin service is unreachable, falls back to
 * the last known config, or to the FREE tier defaults when nothing is cached.
 */
@Component
public class TenantConfigClient {

    private static final Logger log = LoggerFactory.getLogger(TenantConfigClient.class);
    private static final int MAX_CACHE_ENTRIES = 10_000;

    private record Entry(TenantLimitConfig config, long expiresAtMillis) {}

    private final WebClient webClient;
    private final long ttlMillis;
    private final Map<String, Entry> cache = new ConcurrentHashMap<>();

    public TenantConfigClient(
            WebClient.Builder builder,
            @Value("${sentinel.admin.url:http://localhost:8081}") String adminUrl,
            @Value("${sentinel.admin.api-key:}") String adminApiKey,
            @Value("${sentinel.admin.cache-ttl-seconds:30}") long ttlSeconds) {
        WebClient.Builder b = builder.baseUrl(adminUrl);
        if (!adminApiKey.isEmpty()) {
            b = b.defaultHeader("X-Admin-Key", adminApiKey);
        }
        this.webClient = b.build();
        this.ttlMillis = ttlSeconds * 1000;
    }

    /**
     * Config for rate limiting. Never fails: unknown tenants yield empty, and admin-service errors
     * degrade to the last known config or the FREE tier.
     */
    public Mono<TenantLimitConfig> getConfig(String apiKey) {
        Entry cached = cache.get(apiKey);
        if (cached != null && cached.expiresAtMillis() > System.currentTimeMillis()) {
            return Mono.just(cached.config());
        }
        return lookup(apiKey)
                .doOnNext(c -> store(apiKey, c))
                .onErrorResume(e -> fallback(apiKey, cached, e));
    }

    /**
     * Strict lookup that bypasses the cache and propagates errors. Use where a degraded answer would
     * be unsafe, such as issuing tokens. Emits empty when the tenant does not exist.
     */
    public Mono<TenantLimitConfig> lookup(String apiKey) {
        return webClient.get()
                .uri("/api/v1/admin/gateway-config")
                .header("X-Tenant-Api-Key", apiKey)
                .retrieve()
                .bodyToMono(TenantLimitConfig.class)
                .timeout(Duration.ofSeconds(2))
                .onErrorResume(WebClientResponseException.NotFound.class, e -> {
                    cache.remove(apiKey);
                    return Mono.empty();
                });
    }

    private Mono<TenantLimitConfig> fallback(String apiKey, Entry stale, Throwable cause) {
        log.warn("Could not load tenant config from admin service: {}", cause.toString());
        return Mono.just(stale != null ? stale.config() : TenantLimitConfig.forTier(Tier.FREE));
    }

    private void store(String apiKey, TenantLimitConfig config) {
        if (cache.size() >= MAX_CACHE_ENTRIES) {
            cache.clear();
        }
        cache.put(apiKey, new Entry(config, System.currentTimeMillis() + ttlMillis));
    }
}
