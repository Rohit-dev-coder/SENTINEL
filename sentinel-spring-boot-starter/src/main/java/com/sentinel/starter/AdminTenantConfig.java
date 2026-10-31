package com.sentinel.starter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sentinel.common.enums.AlgorithmType;
import com.sentinel.common.enums.Tier;

import java.util.ArrayList;
import java.util.List;

/**
 * A tenant's configuration as returned by the Sentinel admin service
 * ({@code GET /api/v1/admin/gateway-config}).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AdminTenantConfig {

    private boolean active;
    private String tier;
    private int defaultMaxRequests;
    private int defaultWindowSeconds;
    private List<Rule> rules = new ArrayList<>();

    /** Safe low limits used when the admin service cannot be reached and nothing is cached. */
    public static AdminTenantConfig freeTier() {
        AdminTenantConfig config = new AdminTenantConfig();
        config.active = true;
        config.tier = Tier.FREE.name();
        config.defaultMaxRequests = Tier.FREE.getDefaultMaxRequests();
        config.defaultWindowSeconds = Tier.FREE.getDefaultWindowSeconds();
        return config;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Rule {
        private String endpointPattern;
        private AlgorithmType algorithm = AlgorithmType.TOKEN_BUCKET;
        private int maxRequests;
        private int windowSeconds;

        public String getEndpointPattern() { return endpointPattern; }
        public void setEndpointPattern(String endpointPattern) { this.endpointPattern = endpointPattern; }
        public AlgorithmType getAlgorithm() { return algorithm; }
        public void setAlgorithm(AlgorithmType algorithm) { this.algorithm = algorithm; }
        public int getMaxRequests() { return maxRequests; }
        public void setMaxRequests(int maxRequests) { this.maxRequests = maxRequests; }
        public int getWindowSeconds() { return windowSeconds; }
        public void setWindowSeconds(int windowSeconds) { this.windowSeconds = windowSeconds; }
    }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getTier() { return tier; }
    public void setTier(String tier) { this.tier = tier; }
    public int getDefaultMaxRequests() { return defaultMaxRequests; }
    public void setDefaultMaxRequests(int defaultMaxRequests) { this.defaultMaxRequests = defaultMaxRequests; }
    public int getDefaultWindowSeconds() { return defaultWindowSeconds; }
    public void setDefaultWindowSeconds(int defaultWindowSeconds) { this.defaultWindowSeconds = defaultWindowSeconds; }
    public List<Rule> getRules() { return rules == null ? List.of() : rules; }
    public void setRules(List<Rule> rules) { this.rules = rules; }
}
