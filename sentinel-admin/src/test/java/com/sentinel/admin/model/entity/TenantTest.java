package com.sentinel.admin.model.entity;

import com.sentinel.common.enums.Tier;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TenantTest {

    @Test
    void testTenantCreation() {
        Tenant tenant = new Tenant("Test Tenant", "test-api-key", Tier.PREMIUM);
        
        assertNotNull(tenant);
        assertEquals("Test Tenant", tenant.getName());
        assertEquals("test-api-key", tenant.getApiKey());
        assertEquals(Tier.PREMIUM, tenant.getTier());
        assertTrue(tenant.getIsActive());
        assertNotNull(tenant.getCreatedAt());
        assertNotNull(tenant.getUpdatedAt());
    }

    @Test
    void testTenantDefaultValues() {
        Tenant tenant = new Tenant();
        
        assertEquals(Tier.FREE, tenant.getTier());
        assertTrue(tenant.getIsActive());
        assertNull(tenant.getName());
        assertNull(tenant.getApiKey());
    }

    @Test
    void testTenantSettersAndGetters() {
        Tenant tenant = new Tenant();
        UUID id = UUID.randomUUID();
        
        tenant.setId(id);
        tenant.setName("Updated Name");
        tenant.setApiKey("updated-api-key");
        tenant.setTier(Tier.ENTERPRISE);
        tenant.setIsActive(false);
        tenant.setContactEmail("test@example.com");
        
        assertEquals(id, tenant.getId());
        assertEquals("Updated Name", tenant.getName());
        assertEquals("updated-api-key", tenant.getApiKey());
        assertEquals(Tier.ENTERPRISE, tenant.getTier());
        assertFalse(tenant.getIsActive());
        assertEquals("test@example.com", tenant.getContactEmail());
    }

    @Test
    void testTenantTimestamps() {
        Tenant tenant = new Tenant();
        LocalDateTime before = LocalDateTime.now();
        
        // Simulate @PrePersist
        tenant.onCreate();
        
        LocalDateTime after = LocalDateTime.now();
        
        assertNotNull(tenant.getCreatedAt());
        assertNotNull(tenant.getUpdatedAt());
        assertTrue(tenant.getCreatedAt().isAfter(before) || tenant.getCreatedAt().isEqual(before));
        assertTrue(tenant.getCreatedAt().isBefore(after) || tenant.getCreatedAt().isEqual(after));
    }

    @Test
    void testTenantUpdateTimestamp() {
        Tenant tenant = new Tenant();
        tenant.onCreate();
        
        LocalDateTime originalUpdatedAt = tenant.getUpdatedAt();
        
        // Simulate @PreUpdate
        tenant.onUpdate();
        
        assertTrue(tenant.getUpdatedAt().isAfter(originalUpdatedAt));
    }

    @Test
    void testTenantTierValues() {
        Tenant freeTenant = new Tenant("Free", "key1", Tier.FREE);
        assertEquals(Tier.FREE, freeTenant.getTier());
        assertEquals(10, freeTenant.getTier().getDefaultMaxRequests());
        
        Tenant basicTenant = new Tenant("Basic", "key2", Tier.BASIC);
        assertEquals(Tier.BASIC, basicTenant.getTier());
        assertEquals(100, basicTenant.getTier().getDefaultMaxRequests());
        
        Tenant premiumTenant = new Tenant("Premium", "key3", Tier.PREMIUM);
        assertEquals(Tier.PREMIUM, premiumTenant.getTier());
        assertEquals(1000, premiumTenant.getTier().getDefaultMaxRequests());
        
        Tenant enterpriseTenant = new Tenant("Enterprise", "key4", Tier.ENTERPRISE);
        assertEquals(Tier.ENTERPRISE, enterpriseTenant.getTier());
        assertEquals(10000, enterpriseTenant.getTier().getDefaultMaxRequests());
    }
}