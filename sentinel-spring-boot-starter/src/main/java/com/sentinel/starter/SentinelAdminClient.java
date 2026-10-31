package com.sentinel.starter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fetches a tenant's tier and rules from a Sentinel admin service, with a short in-memory cache.
 * <p>
 * {@link #lookup(String)} returns empty when the tenant does not exist. If the admin service cannot be
 * reached it falls back to the last known configuration, or to FREE-tier limits if nothing is cached.
 */
public class SentinelAdminClient {

    private static final Logger log = LoggerFactory.getLogger(SentinelAdminClient.class);
    private static final int MAX_CACHE_ENTRIES = 10_000;

    private record Entry(AdminTenantConfig config, long expiresAtMillis) {
    }

    private final RestClient client;
    private final long ttlMillis;
    private final Map<String, Entry> cache = new ConcurrentHashMap<>();

    public SentinelAdminClient(SentinelProperties.Admin admin) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        int timeoutMillis = (int) admin.getTimeout().toMillis();
        requestFactory.setConnectTimeout(timeoutMillis);
        requestFactory.setReadTimeout(timeoutMillis);

        RestClient.Builder builder = RestClient.builder().baseUrl(admin.getUrl()).requestFactory(requestFactory);
        if (admin.getApiKey() != null && !admin.getApiKey().isBlank()) {
            builder = builder.defaultHeader("X-Admin-Key", admin.getApiKey());
        }
        this.client = builder.build();
        this.ttlMillis = admin.getCacheTtl().toMillis();
    }

    public Optional<AdminTenantConfig> lookup(String tenantApiKey) {
        Entry cached = cache.get(tenantApiKey);
        if (cached != null && cached.expiresAtMillis() > System.currentTimeMillis()) {
            return Optional.of(cached.config());
        }
        try {
            AdminTenantConfig config = client.get()
                    .uri("/api/v1/admin/gateway-config")
                    .header("X-Tenant-Api-Key", tenantApiKey)
                    .retrieve()
                    .body(AdminTenantConfig.class);
            if (config == null) {
                return Optional.empty();
            }
            store(tenantApiKey, config);
            return Optional.of(config);
        } catch (HttpClientErrorException.NotFound e) {
            cache.remove(tenantApiKey);
            return Optional.empty();
        } catch (RuntimeException e) {
            log.warn("Could not load tenant configuration from the Sentinel admin service: {}", e.toString());
            return Optional.of(cached != null ? cached.config() : AdminTenantConfig.freeTier());
        }
    }

    private void store(String key, AdminTenantConfig config) {
        if (cache.size() >= MAX_CACHE_ENTRIES) {
            cache.clear();
        }
        cache.put(key, new Entry(config, System.currentTimeMillis() + ttlMillis));
    }
}
