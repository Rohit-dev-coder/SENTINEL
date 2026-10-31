package com.sentinel.admin.model.dto;

import com.sentinel.common.enums.AlgorithmType;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RuleRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testValidRuleRequest() {
        UUID tenantId = UUID.randomUUID();
        RuleRequest request = new RuleRequest(tenantId, "GET:/api/v1/users/**", 
                                            AlgorithmType.SLIDING_WINDOW_COUNTER, 1000, 60);
        
        assertEquals(tenantId, request.getTenantId());
        assertEquals("GET:/api/v1/users/**", request.getEndpointPattern());
        assertEquals(AlgorithmType.SLIDING_WINDOW_COUNTER, request.getAlgorithm());
        assertEquals(1000, request.getMaxRequests());
        assertEquals(60, request.getWindowSeconds());
    }

    @Test
    void testRuleRequestSettersAndGetters() {
        RuleRequest request = new RuleRequest();
        UUID tenantId = UUID.randomUUID();
        
        request.setTenantId(tenantId);
        request.setEndpointPattern("POST:/api/v1/orders/**");
        request.setAlgorithm(AlgorithmType.FIXED_WINDOW_COUNTER);
        request.setMaxRequests(500);
        request.setWindowSeconds(30);
        
        assertEquals(tenantId, request.getTenantId());
        assertEquals("POST:/api/v1/orders/**", request.getEndpointPattern());
        assertEquals(AlgorithmType.FIXED_WINDOW_COUNTER, request.getAlgorithm());
        assertEquals(500, request.getMaxRequests());
        assertEquals(30, request.getWindowSeconds());
    }

    @Test
    void testRuleRequestValidation() {
        UUID tenantId = UUID.randomUUID();
        RuleRequest request = new RuleRequest(tenantId, "GET:/api/v1/**", 
                                            AlgorithmType.TOKEN_BUCKET, 100, 60);
        
        var violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Valid request should have no violations");
    }

    @Test
    void testRuleRequestNullTenantId() {
        RuleRequest request = new RuleRequest();
        request.setEndpointPattern("GET:/api/v1/**");
        request.setAlgorithm(AlgorithmType.TOKEN_BUCKET);
        request.setMaxRequests(100);
        request.setWindowSeconds(60);
        
        var violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Null tenant ID should have violations");
    }

    @Test
    void testRuleRequestBlankEndpoint() {
        RuleRequest request = new RuleRequest();
        request.setTenantId(UUID.randomUUID());
        request.setEndpointPattern("");
        request.setAlgorithm(AlgorithmType.TOKEN_BUCKET);
        request.setMaxRequests(100);
        request.setWindowSeconds(60);
        
        var violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Blank endpoint should have violations");
    }

    @Test
    void testRuleRequestInvalidMaxRequests() {
        RuleRequest request = new RuleRequest();
        request.setTenantId(UUID.randomUUID());
        request.setEndpointPattern("GET:/api/v1/**");
        request.setAlgorithm(AlgorithmType.TOKEN_BUCKET);
        request.setMaxRequests(0);
        request.setWindowSeconds(60);
        
        var violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Max requests must be at least 1");
    }

    @Test
    void testRuleRequestInvalidWindowSeconds() {
        RuleRequest request = new RuleRequest();
        request.setTenantId(UUID.randomUUID());
        request.setEndpointPattern("GET:/api/v1/**");
        request.setAlgorithm(AlgorithmType.TOKEN_BUCKET);
        request.setMaxRequests(100);
        request.setWindowSeconds(0);
        
        var violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Window seconds must be at least 1");
    }

    @Test
    void testRuleRequestAllAlgorithmTypes() {
        UUID tenantId = UUID.randomUUID();
        
        for (AlgorithmType algorithm : AlgorithmType.values()) {
            RuleRequest request = new RuleRequest(tenantId, "GET:/api/v1/**", 
                                                algorithm, 100, 60);
            assertEquals(algorithm, request.getAlgorithm());
        }
    }
}