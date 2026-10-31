package com.sentinel.starter;

import com.sentinel.common.enums.AlgorithmType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.Ordered;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for the Sentinel rate limiter. Every property is under the {@code sentinel.} prefix.
 */
@ConfigurationProperties(prefix = "sentinel")
public class SentinelProperties {

    /** Turns the rate limiter on or off. */
    private boolean enabled = true;

    /** Request header that identifies the caller (in connected mode: the tenant API key). */
    private String clientIdHeader = "X-API-Key";

    /** What to do when a request carries no client id header. */
    private IdentityFallback identityFallback = IdentityFallback.CLIENT_IP;

    /** If Redis fails: true lets the request through (and logs), false answers 503. */
    private boolean failOpen = true;

    /** Servlet filter order. Lower runs earlier. */
    private int filterOrder = Ordered.HIGHEST_PRECEDENCE + 10;

    /** Ant-style paths that are never rate limited. */
    private List<String> excludePaths = new ArrayList<>(List.of("/actuator/**", "/error"));

    /** Limit used when no rule matches. Ignored in connected mode, where the tenant's tier decides. */
    private final Limit defaultLimit = new Limit();

    /** Per-path limits. The first rule whose pattern matches wins. Ignored in connected mode. */
    private List<Rule> rules = new ArrayList<>();

    /** Optional connection to a Sentinel admin service for centrally managed limits. */
    private final Admin admin = new Admin();

    public enum IdentityFallback {
        /** Use the remote address of the connection. */
        CLIENT_IP,
        /** Answer 401 Unauthorized. */
        REJECT
    }

    /** Throws if the configuration cannot work, so the application fails at startup, not at runtime. */
    public void validate() {
        defaultLimit.validate("sentinel.default-limit");
        for (int i = 0; i < rules.size(); i++) {
            Rule rule = rules.get(i);
            rule.validate("sentinel.rules[" + i + "]");
            if (rule.getPathPattern() == null || !rule.getPathPattern().startsWith("/")) {
                throw new IllegalStateException("sentinel.rules[" + i + "].path-pattern must start with '/'");
            }
        }
        if (clientIdHeader == null || clientIdHeader.isBlank()) {
            throw new IllegalStateException("sentinel.client-id-header must not be blank");
        }
    }

    public static class Limit {
        private AlgorithmType algorithm = AlgorithmType.TOKEN_BUCKET;
        private int maxRequests = 100;
        private int windowSeconds = 60;

        void validate(String name) {
            if (algorithm == null) {
                throw new IllegalStateException(name + ".algorithm must be set");
            }
            if (maxRequests < 1) {
                throw new IllegalStateException(name + ".max-requests must be at least 1");
            }
            if (windowSeconds < 1) {
                throw new IllegalStateException(name + ".window-seconds must be at least 1");
            }
        }

        public AlgorithmType getAlgorithm() { return algorithm; }
        public void setAlgorithm(AlgorithmType algorithm) { this.algorithm = algorithm; }
        public int getMaxRequests() { return maxRequests; }
        public void setMaxRequests(int maxRequests) { this.maxRequests = maxRequests; }
        public int getWindowSeconds() { return windowSeconds; }
        public void setWindowSeconds(int windowSeconds) { this.windowSeconds = windowSeconds; }
    }

    public static class Rule extends Limit {
        /** Ant-style pattern, for example {@code /api/orders/**}. */
        private String pathPattern;

        public String getPathPattern() { return pathPattern; }
        public void setPathPattern(String pathPattern) { this.pathPattern = pathPattern; }
    }

    public static class Admin {
        /** Base URL of the Sentinel admin service. Setting it turns on connected mode. */
        private String url;

        /** Admin API key, sent as X-Admin-Key. */
        private String apiKey;

        /** How long a tenant's configuration is remembered before it is fetched again. */
        private Duration cacheTtl = Duration.ofSeconds(30);

        /** Connect and read timeout for calls to the admin service. */
        private Duration timeout = Duration.ofSeconds(2);

        public boolean isEnabled() {
            return url != null && !url.isBlank();
        }

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public Duration getCacheTtl() { return cacheTtl; }
        public void setCacheTtl(Duration cacheTtl) { this.cacheTtl = cacheTtl; }
        public Duration getTimeout() { return timeout; }
        public void setTimeout(Duration timeout) { this.timeout = timeout; }
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getClientIdHeader() { return clientIdHeader; }
    public void setClientIdHeader(String clientIdHeader) { this.clientIdHeader = clientIdHeader; }
    public IdentityFallback getIdentityFallback() { return identityFallback; }
    public void setIdentityFallback(IdentityFallback identityFallback) { this.identityFallback = identityFallback; }
    public boolean isFailOpen() { return failOpen; }
    public void setFailOpen(boolean failOpen) { this.failOpen = failOpen; }
    public int getFilterOrder() { return filterOrder; }
    public void setFilterOrder(int filterOrder) { this.filterOrder = filterOrder; }
    public List<String> getExcludePaths() { return excludePaths; }
    public void setExcludePaths(List<String> excludePaths) { this.excludePaths = excludePaths; }
    public Limit getDefaultLimit() { return defaultLimit; }
    public List<Rule> getRules() { return rules; }
    public void setRules(List<Rule> rules) { this.rules = rules; }
    public Admin getAdmin() { return admin; }
}
