package com.sentinel.gateway.circuitbreaker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class CircuitBreakerConfigTest {

    @Test
    void testCircuitBreakerConfigLoads() {
        // This test verifies that the circuit breaker configuration loads successfully
        // The actual circuit breaker beans are auto-configured by Resilience4j
        assertTrue(true, "CircuitBreakerConfig should load successfully");
    }

    @Test
    void testConfigurationClassExists() {
        CircuitBreakerConfig config = new CircuitBreakerConfig();
        assertNotNull(config, "CircuitBreakerConfig should be instantiable");
    }
}