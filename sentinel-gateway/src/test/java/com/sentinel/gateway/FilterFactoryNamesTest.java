package com.sentinel.gateway;

import com.sentinel.gateway.ratelimit.RateLimitGatewayFilterFactory;
import com.sentinel.gateway.security.JwtAuthenticationGatewayFilterFactory;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.support.NameUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Routes in application.yml refer to filters by name (JwtAuthentication, RateLimit). Spring Cloud Gateway derives
 * that name by stripping the "GatewayFilterFactory" suffix from the class name, so a misnamed class makes the
 * gateway fail at startup. Unit tests with route-free test configuration cannot catch that, so check it here.
 */
class FilterFactoryNamesTest {

    @Test
    void jwtFilterIsReferencedAsJwtAuthentication() {
        assertEquals("JwtAuthentication", NameUtils.normalizeFilterFactoryName(JwtAuthenticationGatewayFilterFactory.class));
    }

    @Test
    void rateLimitFilterIsReferencedAsRateLimit() {
        assertEquals("RateLimit", NameUtils.normalizeFilterFactoryName(RateLimitGatewayFilterFactory.class));
    }
}
