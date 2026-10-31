package com.sentinel.admin.model.entity;

import com.sentinel.common.enums.AlgorithmType;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitRuleTest {

    @Test
    void testRateLimitRuleCreation() {
        UUID tenantId = UUID.randomUUID();
        RateLimitRule rule = new RateLimitRule(tenantId, "GET:/api/v1/users/**", 
                                            AlgorithmType.SLIDING_WINDOW_COUNTER, 1000, 60);
        
        assertNotNull(rule);
        assertEquals(tenantId, rule.getTenantId());
        assertEquals("GET:/api/v1/users/**", rule.getEndpointPattern());
        assertEquals(AlgorithmType.SLIDING_WINDOW_COUNTER, rule.getAlgorithm());
        assertEquals(1000, rule.getMaxRequests());
        assertEquals(60, rule.getWindowSeconds());
        assertTrue(rule.getIsActive());
        assertNotNull(rule.getCreatedAt());
        assertNotNull(rule.getUpdatedAt());
    }

    @Test
    void testRateLimitRuleDefaultValues() {
        RateLimitRule rule = new RateLimitRule();
        
        assertEquals(AlgorithmType.TOKEN_BUCKET, rule.getAlgorithm());
        assertEquals(100, rule.getMaxRequests());
        assertEquals(60, rule.getWindowSeconds());
        assertTrue(rule.getIsActive());
        assertNull(rule.getTenantId());
        assertNull(rule.getEndpointPattern());
    }

    @Test
    void testRateLimitRuleSettersAndGetters() {
        RateLimitRule rule = new RateLimitRule();
        UUID id = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        
        rule.setId(id);
        rule.setTenantId(tenantId);
        rule.setEndpointPattern("POST:/api/v1/orders/**");
        rule.setAlgorithm(AlgorithmType.FIXED_WINDOW_COUNTER);
        rule.setMaxRequests(500);
        rule.setWindowSeconds(30);
        rule.setIsActive(false);
        
        assertEquals(id, rule.getId());
        assertEquals(tenantId, rule.getTenantId());
        assertEquals("POST:/api/v1/orders/**", rule.getEndpointPattern());
        assertEquals(AlgorithmType.FIXED_WINDOW_COUNTER, rule.getAlgorithm());
        assertEquals(500, rule.getMaxRequests());
        assertEquals(30, rule.getWindowSeconds());
        assertFalse(rule.getIsActive());
    }

    @Test
    void testRateLimitRuleTimestamps() {
        RateLimitRule rule = new RateLimitRule();
        LocalDateTime before = LocalDateTime.now();
        
        rule.onCreate();
        
        LocalDateTime after = LocalDateTime.now();
        
        assertNotNull(rule.getCreatedAt());
        assertNotNull(rule.getUpdatedAt());
        assertTrue(rule.getCreatedAt().isAfter(before) || rule.getCreatedAt().isEqual(before));
        assertTrue(rule.getCreatedAt().isBefore(after) || rule.getCreatedAt().isEqual(after));
    }

    @Test
    void testRateLimitRuleUpdateTimestamp() {
        RateLimitRule rule = new RateLimitRule();
        rule.onCreate();
        
        LocalDateTime originalUpdatedAt = rule.getUpdatedAt();
        
        rule.onUpdate();
        
        assertTrue(rule.getUpdatedAt().isAfter(originalUpdatedAt));
    }

    @Test
    void testRateLimitRuleAlgorithmTypes() {
        UUID tenantId = UUID.randomUUID();
        
        RateLimitRule tokenBucket = new RateLimitRule(tenantId, "GET:/api/v1/**", 
                                                    AlgorithmType.TOKEN_BUCKET, 100, 60);
        assertEquals(AlgorithmType.TOKEN_BUCKET, tokenBucket.getAlgorithm());
        
        RateLimitRule slidingLog = new RateLimitRule(tenantId, "POST:/api/v1/**", 
                                                    AlgorithmType.SLIDING_WINDOW_LOG, 50, 60);
        assertEquals(AlgorithmType.SLIDING_WINDOW_LOG, slidingLog.getAlgorithm());
        
        RateLimitRule fixedWindow = new RateLimitRule(tenantId, "PUT:/api/v1/**", 
                                                      AlgorithmType.FIXED_WINDOW_COUNTER, 200, 60);
        assertEquals(AlgorithmType.FIXED_WINDOW_COUNTER, fixedWindow.getAlgorithm());
        
        RateLimitRule slidingCounter = new RateLimitRule(tenantId, "DELETE:/api/v1/**", 
                                                        AlgorithmType.SLIDING_WINDOW_COUNTER, 25, 60);
        assertEquals(AlgorithmType.SLIDING_WINDOW_COUNTER, slidingCounter.getAlgorithm());
    }

    @Test
    void testRateLimitRuleValidation() {
        RateLimitRule rule = new RateLimitRule();
        
        // Valid values
        rule.setMaxRequests(1);
        rule.setWindowSeconds(1);
        assertEquals(1, rule.getMaxRequests());
        assertEquals(1, rule.getWindowSeconds());
        
        // High values
        rule.setMaxRequests(100000);
        rule.setWindowSeconds(86400); // 1 day
        assertEquals(100000, rule.getMaxRequests());
        assertEquals(86400, rule.getWindowSeconds());
    }
}