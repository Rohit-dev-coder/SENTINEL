package com.sentinel.common.enums;

/**
 * Rate limiting algorithm types supported by SENTINEL.
 */
public enum AlgorithmType {
    TOKEN_BUCKET("Token Bucket"),
    SLIDING_WINDOW_LOG("Sliding Window Log"),
    FIXED_WINDOW_COUNTER("Fixed Window Counter"),
    SLIDING_WINDOW_COUNTER("Sliding Window Counter");

    private final String displayName;

    AlgorithmType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static AlgorithmType fromString(String value) {
        for (AlgorithmType type : AlgorithmType.values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown algorithm type: " + value);
    }
}