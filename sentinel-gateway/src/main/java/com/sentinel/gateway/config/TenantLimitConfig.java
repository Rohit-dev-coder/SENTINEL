package com.sentinel.gateway.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sentinel.common.enums.AlgorithmType;
import com.sentinel.common.enums.Tier;

import java.util.List;

/**
 * Gateway-side view of the admin service's per-tenant configuration.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TenantLimitConfig {

    private boolean active;
    private Tier tier;
    private int defaultMaxRequests;
    private int defaultWindowSeconds;
    private List<Rule> rules = List.of();

    public static TenantLimitConfig forTier(Tier tier) {
        TenantLimitConfig c = new TenantLimitConfig();
        c.active = true;
        c.tier = tier;
        c.defaultMaxRequests = tier.getDefaultMaxRequests();
        c.defaultWindowSeconds = tier.getDefaultWindowSeconds();
        return c;
    }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Tier getTier() { return tier; }
    public void setTier(Tier tier) { this.tier = tier; }
    public int getDefaultMaxRequests() { return defaultMaxRequests; }
    public void setDefaultMaxRequests(int v) { this.defaultMaxRequests = v; }
    public int getDefaultWindowSeconds() { return defaultWindowSeconds; }
    public void setDefaultWindowSeconds(int v) { this.defaultWindowSeconds = v; }
    public List<Rule> getRules() { return rules == null ? List.of() : rules; }
    public void setRules(List<Rule> rules) { this.rules = rules; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Rule {
        private String endpointPattern;
        private AlgorithmType algorithm = AlgorithmType.TOKEN_BUCKET;
        private int maxRequests;
        private int windowSeconds;

        public String getEndpointPattern() { return endpointPattern; }
        public void setEndpointPattern(String p) { this.endpointPattern = p; }
        public AlgorithmType getAlgorithm() { return algorithm; }
        public void setAlgorithm(AlgorithmType a) { this.algorithm = a; }
        public int getMaxRequests() { return maxRequests; }
        public void setMaxRequests(int m) { this.maxRequests = m; }
        public int getWindowSeconds() { return windowSeconds; }
        public void setWindowSeconds(int w) { this.windowSeconds = w; }
    }
}
