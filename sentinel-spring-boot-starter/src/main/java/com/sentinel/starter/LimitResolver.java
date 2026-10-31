package com.sentinel.starter;

import org.springframework.util.AntPathMatcher;

import java.util.List;

/** Picks the limit for a request path: the first matching rule, otherwise the fallback. */
public class LimitResolver {

    /** A path pattern and the limit it grants. */
    public record RuleSpec(String pathPattern, EffectiveLimit limit) {
    }

    private final AntPathMatcher matcher = new AntPathMatcher();

    public EffectiveLimit resolve(List<RuleSpec> rules, EffectiveLimit fallback, String path) {
        for (RuleSpec rule : rules) {
            if (rule.pathPattern() != null && matcher.match(rule.pathPattern(), path)) {
                return rule.limit();
            }
        }
        return fallback;
    }
}
