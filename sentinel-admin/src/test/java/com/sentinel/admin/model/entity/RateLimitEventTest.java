package com.sentinel.admin.model.entity;

import com.sentinel.common.enums.AlgorithmType;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitEventTest {

    @Test
    void testRateLimitEventCreation() {
        UUID tenantId = UUID.randomUUID();
        RateLimitEvent event = new RateLimitEvent(tenantId, "GET:/api/v1/users", 
                                                AlgorithmType.TOKEN_BUCKET, true, 95, "192.168.1.1");
        
        assertNotNull(event);
        assertEquals(tenantId, event.getTenantId());
        assertEquals("GET:/api/v1/users", event.getEndpoint());
        assertEquals(AlgorithmType.TOKEN_BUCKET, event.getAlgorithm());
        assertTrue(event.getAllowed());
        assertEquals(95, event.getRemaining());
        assertEquals("192.168.1.1", event.getIpAddress());
        assertNotNull(event.getCreatedAt());
    }

    @Test
    void testRateLimitEventDefaultValues() {
        RateLimitEvent event = new RateLimitEvent();
        
        assertNull(event.getTenantId());
        assertNull(event.getEndpoint());
        assertNull(event.getAlgorithm());
        assertNull(event.getAllowed());
        assertNull(event.getRemaining());
        assertNull(event.getIpAddress());
    }

    @Test
    void testRateLimitEventSettersAndGetters() {
        RateLimitEvent event = new RateLimitEvent();
        UUID id = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        
        event.setId(id);
        event.setTenantId(tenantId);
        event.setEndpoint("POST:/api/v1/orders");
        event.setAlgorithm(AlgorithmType.SLIDING_WINDOW_LOG);
        event.setAllowed(false);
        event.setRemaining(0);
        event.setIpAddress("10.0.0.1");
        
        assertEquals(id, event.getId());
        assertEquals(tenantId, event.getTenantId());
        assertEquals("POST:/api/v1/orders", event.getEndpoint());
        assertEquals(AlgorithmType.SLIDING_WINDOW_LOG, event.getAlgorithm());
        assertFalse(event.getAllowed());
        assertEquals(0, event.getRemaining());
        assertEquals("10.0.0.1", event.getIpAddress());
    }

    @Test
    void testRateLimitEventTimestamps() {
        RateLimitEvent event = new RateLimitEvent();
        LocalDateTime before = LocalDateTime.now();
        
        event.onCreate();
        
        LocalDateTime after = LocalDateTime.now();
        
        assertNotNull(event.getCreatedAt());
        assertTrue(event.getCreatedAt().isAfter(before) || event.getCreatedAt().isEqual(before));
        assertTrue(event.getCreatedAt().isBefore(after) || event.getCreatedAt().isEqual(after));
    }

    @Test
    void testRateLimitEventAllowedAndDenied() {
        UUID tenantId = UUID.randomUUID();
        
        RateLimitEvent allowedEvent = new RateLimitEvent(tenantId, "GET:/api/v1/users", 
                                                        AlgorithmType.TOKEN_BUCKET, true, 99, "192.168.1.1");
        assertTrue(allowedEvent.getAllowed());
        assertEquals(99, allowedEvent.getRemaining());
        
        RateLimitEvent deniedEvent = new RateLimitEvent(tenantId, "GET:/api/v1/users", 
                                                       AlgorithmType.TOKEN_BUCKET, false, 0, "192.168.1.1");
        assertFalse(deniedEvent.getAllowed());
        assertEquals(0, deniedEvent.getRemaining());
    }

    @Test
    void testRateLimitEventWithNullValues() {
        RateLimitEvent event = new RateLimitEvent(UUID.randomUUID(), "GET:/api/v1/users", 
                                                AlgorithmType.TOKEN_BUCKET, true, 100, null);
        
        assertNotNull(event);
        assertTrue(event.getAllowed());
        assertEquals(100, event.getRemaining());
        assertNull(event.getIpAddress());
    }

    @Test
    void testRateLimitEventAlgorithmTypes() {
        UUID tenantId = UUID.randomUUID();
        
        for (AlgorithmType algorithm : AlgorithmType.values()) {
            RateLimitEvent event = new RateLimitEvent(tenantId, "GET:/api/v1/users", 
                                                    algorithm, true, 50, "127.0.0.1");
            assertEquals(algorithm, event.getAlgorithm());
        }
    }
}