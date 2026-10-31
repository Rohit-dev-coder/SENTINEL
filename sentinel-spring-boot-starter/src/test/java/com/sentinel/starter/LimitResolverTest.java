package com.sentinel.starter;

import com.sentinel.common.enums.AlgorithmType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class LimitResolverTest {

    private final LimitResolver resolver = new LimitResolver();
    private final EffectiveLimit fallback = new EffectiveLimit(AlgorithmType.TOKEN_BUCKET, 100, 60);

    @Test
    void usesFallbackWhenNoRuleMatches() {
        EffectiveLimit result = resolver.resolve(
                List.of(new LimitResolver.RuleSpec("/api/orders/**", new EffectiveLimit(AlgorithmType.FIXED_WINDOW_COUNTER, 5, 30))),
                fallback, "/api/users/1");

        assertSame(fallback, result);
    }

    @Test
    void firstMatchingRuleWins() {
        EffectiveLimit first = new EffectiveLimit(AlgorithmType.FIXED_WINDOW_COUNTER, 5, 30);
        EffectiveLimit second = new EffectiveLimit(AlgorithmType.SLIDING_WINDOW_LOG, 9, 10);

        EffectiveLimit result = resolver.resolve(List.of(
                new LimitResolver.RuleSpec("/api/orders/**", first),
                new LimitResolver.RuleSpec("/api/**", second)), fallback, "/api/orders/42");

        assertEquals(first, result);
    }

    @Test
    void singleStarMatchesOneSegmentOnly() {
        EffectiveLimit rule = new EffectiveLimit(AlgorithmType.TOKEN_BUCKET, 1, 1);
        List<LimitResolver.RuleSpec> rules = List.of(new LimitResolver.RuleSpec("/api/users/*", rule));

        assertEquals(rule, resolver.resolve(rules, fallback, "/api/users/7"));
        assertSame(fallback, resolver.resolve(rules, fallback, "/api/users/7/orders"));
    }
}
