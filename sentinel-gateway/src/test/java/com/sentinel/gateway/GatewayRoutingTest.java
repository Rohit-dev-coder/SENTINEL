package com.sentinel.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class GatewayRoutingTest {

    @Test
    void testApplicationContextLoads() {
        // This test verifies that the Spring application context loads successfully
        assertTrue(true, "Application context loaded successfully");
    }
}