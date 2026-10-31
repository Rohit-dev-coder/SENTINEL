package com.sentinel.admin.service;

import com.sentinel.admin.cache.RedisConfigCache;
import com.sentinel.admin.model.dto.TenantRequest;
import com.sentinel.admin.model.dto.TenantResponse;
import com.sentinel.admin.model.entity.Tenant;
import com.sentinel.admin.repository.TenantRepository;
import com.sentinel.common.enums.Tier;
import com.sentinel.admin.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class TenantService {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired(required = false)
    private RedisConfigCache redisConfigCache;

    public TenantResponse createTenant(TenantRequest request) {
        // Generate once so the uniqueness check applies to the key we actually save
        String apiKey = generateApiKey(request.getName());
        if (tenantRepository.existsByApiKey(apiKey)) {
            throw new IllegalArgumentException("API key already exists");
        }

        Tenant tenant = new Tenant(
            request.getName(),
            apiKey,
            request.getTier()
        );
        tenant.setContactEmail(request.getContactEmail());

        Tenant savedTenant = tenantRepository.save(tenant);
        TenantResponse response = mapToResponse(savedTenant);
        
        // Cache the new tenant if Redis is available
        if (redisConfigCache != null) {
            redisConfigCache.cacheTenant(response);
            redisConfigCache.cacheTenantByApiKey(response.getApiKey(), response);
        }
        
        return response;
    }

    public TenantResponse getTenantById(UUID id) {
        // Try cache first if Redis is available
        if (redisConfigCache != null) {
            TenantResponse cached = redisConfigCache.getCachedTenant(id);
            if (cached != null) {
                return cached;
            }
        }

        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found: " + id));
        TenantResponse response = mapToResponse(tenant);
        
        // Cache the tenant if Redis is available
        if (redisConfigCache != null) {
            redisConfigCache.cacheTenant(response);
        }
        
        return response;
    }

    public TenantResponse getTenantByApiKey(String apiKey) {
        // Try cache first if Redis is available
        if (redisConfigCache != null) {
            TenantResponse cached = redisConfigCache.getCachedTenantByApiKey(apiKey);
            if (cached != null) {
                return cached;
            }
        }

        Tenant tenant = tenantRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found for the provided API key"));
        TenantResponse response = mapToResponse(tenant);
        
        // Cache the tenant if Redis is available
        if (redisConfigCache != null) {
            redisConfigCache.cacheTenantByApiKey(apiKey, response);
        }
        
        return response;
    }

    public List<TenantResponse> getAllTenants() {
        return tenantRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<TenantResponse> getActiveTenants() {
        return tenantRepository.findByIsActiveTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<TenantResponse> getTenantsByTier(Tier tier) {
        return tenantRepository.findByTier(tier).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public TenantResponse updateTenant(UUID id, TenantRequest request) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found: " + id));

        tenant.setName(request.getName());
        tenant.setTier(request.getTier());
        tenant.setContactEmail(request.getContactEmail());

        Tenant updatedTenant = tenantRepository.save(tenant);
        TenantResponse response = mapToResponse(updatedTenant);
        
        // Update cache if Redis is available
        if (redisConfigCache != null) {
            redisConfigCache.evictAllTenantData(id);
            redisConfigCache.cacheTenant(response);
            redisConfigCache.cacheTenantByApiKey(response.getApiKey(), response);
        }
        
        return response;
    }

    public void deleteTenant(UUID id) {
        if (!tenantRepository.existsById(id)) {
            throw new ResourceNotFoundException("Tenant not found: " + id);
        }
        
        // Evict from cache if Redis is available
        if (redisConfigCache != null) {
            tenantRepository.findById(id)
                    .ifPresent(t -> redisConfigCache.evictTenantByApiKey(t.getApiKey()));
            redisConfigCache.evictAllTenantData(id);
        }
        
        tenantRepository.deleteById(id);
    }

    public void activateTenant(UUID id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found: " + id));
        tenant.setIsActive(true);
        Tenant updatedTenant = tenantRepository.save(tenant);
        
        // Update cache if Redis is available
        if (redisConfigCache != null) {
            TenantResponse refreshed = mapToResponse(updatedTenant);
            redisConfigCache.evictAllTenantData(id);
            redisConfigCache.cacheTenant(refreshed);
            redisConfigCache.cacheTenantByApiKey(refreshed.getApiKey(), refreshed);
        }
    }

    public void deactivateTenant(UUID id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found: " + id));
        tenant.setIsActive(false);
        Tenant updatedTenant = tenantRepository.save(tenant);
        
        // Update cache if Redis is available
        if (redisConfigCache != null) {
            TenantResponse refreshed = mapToResponse(updatedTenant);
            redisConfigCache.evictAllTenantData(id);
            redisConfigCache.cacheTenant(refreshed);
            redisConfigCache.cacheTenantByApiKey(refreshed.getApiKey(), refreshed);
        }
    }

    private static final java.security.SecureRandom RANDOM = new java.security.SecureRandom();

    /** 192 bits from a CSPRNG; the key is a credential, so it must not be guessable from the tenant name. */
    private String generateApiKey(String name) {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return "sk_" + java.util.HexFormat.of().formatHex(bytes);
    }

    private TenantResponse mapToResponse(Tenant tenant) {
        return new TenantResponse(
            tenant.getId(),
            tenant.getName(),
            tenant.getApiKey(),
            tenant.getTier(),
            tenant.getIsActive(),
            tenant.getContactEmail(),
            tenant.getCreatedAt(),
            tenant.getUpdatedAt()
        );
    }
}