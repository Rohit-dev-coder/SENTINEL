package com.sentinel.starter;

import com.sentinel.common.enums.AlgorithmType;

/** The limit that applies to one request after rules and defaults have been resolved. */
public record EffectiveLimit(AlgorithmType algorithm, int maxRequests, int windowSeconds) {
}
