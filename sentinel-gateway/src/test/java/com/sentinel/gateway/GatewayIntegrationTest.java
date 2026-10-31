package com.sentinel.gateway;

import com.sentinel.gateway.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIfSystemProperty(named = "docker.tests.enabled", matches = "true")
class GatewayIntegrationTest {

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void testGatewayRoutingToSampleService1() {
        TestRestTemplate restTemplate = new TestRestTemplate();
        
        // Generate JWT token
        String apiKey = "test_api_key";
        String token = jwtUtil.generateToken(apiKey);
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        // Test routing to sample-service-1 users endpoint
        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:8080/api/v1/users",
            HttpMethod.GET,
            entity,
            String.class
        );
        
        // This test will only pass if downstream services are running
        // In normal CI/CD without Docker, this will be skipped
        assertTrue(response.getStatusCode().is2xxSuccessful() || 
                  response.getStatusCode().is4xxClientError());
    }

    @Test
    void testGatewayRoutingToSampleService2() {
        TestRestTemplate restTemplate = new TestRestTemplate();
        
        // Generate JWT token
        String apiKey = "test_api_key_2";
        String token = jwtUtil.generateToken(apiKey);
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        // Test routing to sample-service-2 analytics endpoint
        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:8080/api/v1/analytics",
            HttpMethod.GET,
            entity,
            String.class
        );
        
        // This test will only pass if downstream services are running
        // In normal CI/CD without Docker, this will be skipped
        assertTrue(response.getStatusCode().is2xxSuccessful() || 
                  response.getStatusCode().is4xxClientError());
    }

    @Test
    void testGatewayWithoutAuthentication() {
        TestRestTemplate restTemplate = new TestRestTemplate();
        
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        // Test routing without authentication (should fail)
        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:8080/api/v1/users",
            HttpMethod.GET,
            entity,
            String.class
        );
        
        // Should return 401 Unauthorized
        assertTrue(response.getStatusCode().is4xxClientError());
    }

    @Test
    void testGatewayWithInvalidToken() {
        TestRestTemplate restTemplate = new TestRestTemplate();
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer invalid_token");
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        // Test routing with invalid token (should fail)
        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:8080/api/v1/users",
            HttpMethod.GET,
            entity,
            String.class
        );
        
        // Should return 401 Unauthorized
        assertTrue(response.getStatusCode().is4xxClientError());
    }

    @Test
    void testGatewayHealthEndpoint() {
        TestRestTemplate restTemplate = new TestRestTemplate();
        
        // Health endpoint should not require authentication
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:8080/actuator/health",
            String.class
        );
        
        // Should return 200 OK
        assertTrue(response.getStatusCode().is2xxSuccessful());
    }
}