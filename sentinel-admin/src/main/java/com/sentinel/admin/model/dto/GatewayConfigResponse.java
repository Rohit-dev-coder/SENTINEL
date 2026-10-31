package com.sentinel.admin.model.dto;

import com.sentinel.common.enums.Tier;

import java.util.List;

/**
 * Per-tenant configuration consumed by the gateway: tier defaults plus active rules.
 */
public class GatewayConfigResponse {

    private boolean active;
    private Tier tier;
    private int defaultMaxRequests;
    private int defaultWindowSeconds;
    private List<RuleResponse> rules;

    public GatewayConfigResponse() {
    }

    public GatewayConfigResponse(boolean active, Tier tier, List<RuleResponse> rules) {
        this.active = active;
        this.tier = tier;
        this.defaultMaxRequests = tier.getDefaultMaxRequests();
        this.defaultWindowSeconds = tier.getDefaultWindowSeconds();
        this.rules = rules;
    }

    public boolean isActive() {
        return active;
    }

    public Tier getTier() {
        return tier;
    }

    public int getDefaultMaxRequests() {
        return defaultMaxRequests;
    }

    public int getDefaultWindowSeconds() {
        return defaultWindowSeconds;
    }

    public List<RuleResponse> getRules() {
        return rules;
    }
}
