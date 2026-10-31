package com.sentinel.admin.service;

import com.sentinel.admin.cache.RedisConfigCache;
import com.sentinel.admin.model.dto.TenantRequest;
import com.sentinel.admin.model.dto.TenantResponse;
import com.sentinel.admin.model.entity.Tenant;
import com.sentinel.admin.repository.TenantRepository;
import com.sentinel.common.enums.Tier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private RedisConfigCache redisConfigCache;

    @InjectMocks
    private TenantService tenantService;

    @Test
    void testCreateTenant() {
        TenantRequest request = new TenantRequest("Test Tenant", Tier.PREMIUM, "test@example.com");
        Tenant savedTenant = new Tenant("Test Tenant", "sk_test_tenant_abc123", Tier.PREMIUM);
        savedTenant.setId(UUID.randomUUID());
        savedTenant.setContactEmail("test@example.com");

        when(tenantRepository.existsByApiKey(anyString())).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenReturn(savedTenant);

        TenantResponse response = tenantService.createTenant(request);

        assertNotNull(response);
        assertEquals("Test Tenant", response.getName());
        assertEquals(Tier.PREMIUM, response.getTier());
        assertEquals("test@example.com", response.getContactEmail());
        verify(tenantRepository).save(any(Tenant.class));
        verify(redisConfigCache).cacheTenant(any(TenantResponse.class));
        verify(redisConfigCache).cacheTenantByApiKey(anyString(), any(TenantResponse.class));
    }

    @Test
    void testCreateTenantDuplicateApiKey() {
        TenantRequest request = new TenantRequest("Test Tenant", Tier.PREMIUM, "test@example.com");

        when(tenantRepository.existsByApiKey(anyString())).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> tenantService.createTenant(request));
        verify(tenantRepository, never()).save(any(Tenant.class));
    }

    @Test
    void testGetTenantById() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant("Test Tenant", "test-key", Tier.BASIC);
        tenant.setId(tenantId);

        when(redisConfigCache.getCachedTenant(tenantId)).thenReturn(null);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        TenantResponse response = tenantService.getTenantById(tenantId);

        assertNotNull(response);
        assertEquals(tenantId, response.getId());
        assertEquals("Test Tenant", response.getName());
        verify(redisConfigCache).getCachedTenant(tenantId);
        verify(tenantRepository).findById(tenantId);
        verify(redisConfigCache).cacheTenant(any(TenantResponse.class));
    }

    @Test
    void testGetTenantByIdFromCache() {
        UUID tenantId = UUID.randomUUID();
        TenantResponse cachedResponse = new TenantResponse(tenantId, "Test Tenant", "test-key", Tier.BASIC, true, "test@example.com", null, null);

        when(redisConfigCache.getCachedTenant(tenantId)).thenReturn(cachedResponse);

        TenantResponse response = tenantService.getTenantById(tenantId);

        assertNotNull(response);
        assertEquals(tenantId, response.getId());
        assertEquals("Test Tenant", response.getName());
        verify(redisConfigCache).getCachedTenant(tenantId);
        verify(tenantRepository, never()).findById(any());
    }

    @Test
    void testGetTenantByIdNotFound() {
        UUID tenantId = UUID.randomUUID();

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> tenantService.getTenantById(tenantId));
        verify(tenantRepository).findById(tenantId);
    }

    @Test
    void testGetTenantByApiKey() {
        String apiKey = "test-api-key";
        Tenant tenant = new Tenant("Test Tenant", apiKey, Tier.FREE);
        tenant.setId(UUID.randomUUID());

        when(redisConfigCache.getCachedTenantByApiKey(apiKey)).thenReturn(null);
        when(tenantRepository.findByApiKey(apiKey)).thenReturn(Optional.of(tenant));

        TenantResponse response = tenantService.getTenantByApiKey(apiKey);

        assertNotNull(response);
        assertEquals(apiKey, response.getApiKey());
        verify(redisConfigCache).getCachedTenantByApiKey(apiKey);
        verify(tenantRepository).findByApiKey(apiKey);
        verify(redisConfigCache).cacheTenantByApiKey(eq(apiKey), any(TenantResponse.class));
    }

    @Test
    void testGetAllTenants() {
        Tenant tenant1 = new Tenant("Tenant 1", "key1", Tier.FREE);
        tenant1.setId(UUID.randomUUID());
        Tenant tenant2 = new Tenant("Tenant 2", "key2", Tier.PREMIUM);
        tenant2.setId(UUID.randomUUID());

        when(tenantRepository.findAll()).thenReturn(Arrays.asList(tenant1, tenant2));

        List<TenantResponse> responses = tenantService.getAllTenants();

        assertEquals(2, responses.size());
        verify(tenantRepository).findAll();
    }

    @Test
    void testGetActiveTenants() {
        Tenant activeTenant = new Tenant("Active", "key1", Tier.BASIC);
        activeTenant.setId(UUID.randomUUID());
        activeTenant.setIsActive(true);

        when(tenantRepository.findByIsActiveTrue()).thenReturn(Arrays.asList(activeTenant));

        List<TenantResponse> responses = tenantService.getActiveTenants();

        assertEquals(1, responses.size());
        assertTrue(responses.get(0).getIsActive());
        verify(tenantRepository).findByIsActiveTrue();
    }

    @Test
    void testGetTenantsByTier() {
        Tenant freeTenant = new Tenant("Free", "key1", Tier.FREE);
        freeTenant.setId(UUID.randomUUID());

        when(tenantRepository.findByTier(Tier.FREE)).thenReturn(Arrays.asList(freeTenant));

        List<TenantResponse> responses = tenantService.getTenantsByTier(Tier.FREE);

        assertEquals(1, responses.size());
        assertEquals(Tier.FREE, responses.get(0).getTier());
        verify(tenantRepository).findByTier(Tier.FREE);
    }

    @Test
    void testUpdateTenant() {
        UUID tenantId = UUID.randomUUID();
        Tenant existingTenant = new Tenant("Old Name", "old-key", Tier.FREE);
        existingTenant.setId(tenantId);

        Tenant updatedTenant = new Tenant("New Name", "old-key", Tier.PREMIUM);
        updatedTenant.setId(tenantId);

        TenantRequest request = new TenantRequest("New Name", Tier.PREMIUM, "new@example.com");

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(existingTenant));
        when(tenantRepository.save(any(Tenant.class))).thenReturn(updatedTenant);

        TenantResponse response = tenantService.updateTenant(tenantId, request);

        assertEquals("New Name", response.getName());
        assertEquals(Tier.PREMIUM, response.getTier());
        verify(tenantRepository).save(any(Tenant.class));
        verify(redisConfigCache).evictAllTenantData(tenantId);
        verify(redisConfigCache).cacheTenant(any(TenantResponse.class));
        verify(redisConfigCache).cacheTenantByApiKey(anyString(), any(TenantResponse.class));
    }

    @Test
    void testDeleteTenant() {
        UUID tenantId = UUID.randomUUID();

        when(tenantRepository.existsById(tenantId)).thenReturn(true);
        doNothing().when(tenantRepository).deleteById(tenantId);

        tenantService.deleteTenant(tenantId);

        verify(tenantRepository).existsById(tenantId);
        verify(tenantRepository).deleteById(tenantId);
        verify(redisConfigCache).evictAllTenantData(tenantId);
    }

    @Test
    void testDeleteTenantNotFound() {
        UUID tenantId = UUID.randomUUID();

        when(tenantRepository.existsById(tenantId)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> tenantService.deleteTenant(tenantId));
        verify(tenantRepository).existsById(tenantId);
        verify(tenantRepository, never()).deleteById(any());
    }

    @Test
    void testActivateTenant() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant("Test", "key", Tier.BASIC);
        tenant.setId(tenantId);
        tenant.setIsActive(false);

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(Tenant.class))).thenReturn(tenant);

        tenantService.activateTenant(tenantId);

        assertTrue(tenant.getIsActive());
        verify(tenantRepository).save(tenant);
        verify(redisConfigCache).evictAllTenantData(tenantId);
        verify(redisConfigCache).cacheTenant(any(TenantResponse.class));
    }

    @Test
    void testDeactivateTenant() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant("Test", "key", Tier.BASIC);
        tenant.setId(tenantId);
        tenant.setIsActive(true);

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(Tenant.class))).thenReturn(tenant);

        tenantService.deactivateTenant(tenantId);

        assertFalse(tenant.getIsActive());
        verify(tenantRepository).save(tenant);
        verify(redisConfigCache).evictAllTenantData(tenantId);
        verify(redisConfigCache).cacheTenant(any(TenantResponse.class));
    }
}