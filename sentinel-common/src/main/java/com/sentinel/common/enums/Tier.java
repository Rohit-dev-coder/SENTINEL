package com.sentinel.common.enums;

/**
 * Tenant tiers with different rate limit limits.
 */
public enum Tier {
    FREE(10, 60),
    BASIC(100, 60),
    PREMIUM(1000, 60),
    ENTERPRISE(10000, 60);

    private final int defaultMaxRequests;
    private final int defaultWindowSeconds;

    Tier(int defaultMaxRequests, int defaultWindowSeconds) {
        this.defaultMaxRequests = defaultMaxRequests;
        this.defaultWindowSeconds = defaultWindowSeconds;
    }

    public int getDefaultMaxRequests() {
        return defaultMaxRequests;
    }

    public int getDefaultWindowSeconds() {
        return defaultWindowSeconds;
    }

    public static Tier fromString(String value) {
        for (Tier tier : Tier.values()) {
            if (tier.name().equalsIgnoreCase(value)) {
                return tier;
            }
        }
        throw new IllegalArgumentException("Unknown tier: " + value);
    }
}