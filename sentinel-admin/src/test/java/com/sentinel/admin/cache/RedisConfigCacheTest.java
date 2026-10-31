package com.sentinel.admin.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.admin.model.dto.RuleResponse;
import com.sentinel.admin.model.dto.TenantResponse;
import com.sentinel.common.enums.AlgorithmType;
import com.sentinel.common.enums.Tier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisConfigCacheTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private RedisConfigCache redisConfigCache;

    @Test
    void testCacheTenant() {
        UUID tenantId = UUID.randomUUID();
        TenantResponse tenant = new TenantResponse(tenantId, "Test Tenant", "sk_test_abc123", Tier.PREMIUM, true, "test@example.com", LocalDateTime.now(), LocalDateTime.now());

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        redisConfigCache.cacheTenant(tenant);

        verify(valueOperations).set(eq("tenant:" + tenantId), eq(tenant), eq(24L), eq(TimeUnit.HOURS));
    }

    @Test
    void testGetCachedTenant() {
        UUID tenantId = UUID.randomUUID();
        TenantResponse tenant = new TenantResponse(tenantId, "Test Tenant", "sk_test_abc123", Tier.PREMIUM, true, "test@example.com", LocalDateTime.now(), LocalDateTime.now());

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("tenant:" + tenantId)).thenReturn(tenant);

        TenantResponse result = redisConfigCache.getCachedTenant(tenantId);

        assertNotNull(result);
        assertEquals(tenantId, result.getId());
        verify(valueOperations).get("tenant:" + tenantId);
    }

    @Test
    void testGetCachedTenantNotFound() {
        UUID tenantId = UUID.randomUUID();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("tenant:" + tenantId)).thenReturn(null);

        TenantResponse result = redisConfigCache.getCachedTenant(tenantId);

        assertNull(result);
        verify(valueOperations).get("tenant:" + tenantId);
    }

    @Test
    void testCacheTenantByApiKey() {
        String apiKey = "sk_test_abc123";
        TenantResponse tenant = new TenantResponse(UUID.randomUUID(), "Test Tenant", apiKey, Tier.PREMIUM, true, "test@example.com", LocalDateTime.now(), LocalDateTime.now());

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        redisConfigCache.cacheTenantByApiKey(apiKey, tenant);

        verify(valueOperations).set(eq("tenant:api:" + apiKey), eq(tenant), eq(24L), eq(TimeUnit.HOURS));
    }

    @Test
    void testGetCachedTenantByApiKey() {
        String apiKey = "sk_test_abc123";
        TenantResponse tenant = new TenantResponse(UUID.randomUUID(), "Test Tenant", apiKey, Tier.PREMIUM, true, "test@example.com", LocalDateTime.now(), LocalDateTime.now());

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("tenant:api:" + apiKey)).thenReturn(tenant);

        TenantResponse result = redisConfigCache.getCachedTenantByApiKey(apiKey);

        assertNotNull(result);
        assertEquals(apiKey, result.getApiKey());
        verify(valueOperations).get("tenant:api:" + apiKey);
    }

    @Test
    void testCacheRule() {
        UUID ruleId = UUID.randomUUID();
        RuleResponse rule = new RuleResponse(ruleId, UUID.randomUUID(), "GET:/api/v1/**", AlgorithmType.TOKEN_BUCKET, 100, 60, true, LocalDateTime.now(), LocalDateTime.now());

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        redisConfigCache.cacheRule(rule);

        verify(valueOperations).set(eq("rule:" + ruleId), eq(rule), eq(24L), eq(TimeUnit.HOURS));
    }

    @Test
    void testGetCachedRule() {
        UUID ruleId = UUID.randomUUID();
        RuleResponse rule = new RuleResponse(ruleId, UUID.randomUUID(), "GET:/api/v1/**", AlgorithmType.TOKEN_BUCKET, 100, 60, true, LocalDateTime.now(), LocalDateTime.now());

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("rule:" + ruleId)).thenReturn(rule);

        RuleResponse result = redisConfigCache.getCachedRule(ruleId);

        assertNotNull(result);
        assertEquals(ruleId, result.getId());
        verify(valueOperations).get("rule:" + ruleId);
    }

    @Test
    void testCacheTenantRules() {
        UUID tenantId = UUID.randomUUID();
        List<RuleResponse> rules = Arrays.asList(
            new RuleResponse(UUID.randomUUID(), tenantId, "GET:/api/v1/**", AlgorithmType.TOKEN_BUCKET, 100, 60, true, LocalDateTime.now(), LocalDateTime.now()),
            new RuleResponse(UUID.randomUUID(), tenantId, "POST:/api/v1/**", AlgorithmType.SLIDING_WINDOW_LOG, 50, 60, true, LocalDateTime.now(), LocalDateTime.now())
        );

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        redisConfigCache.cacheTenantRules(tenantId, rules);

        verify(valueOperations).set(eq("tenant_rules:" + tenantId), eq(rules), eq(24L), eq(TimeUnit.HOURS));
    }

    @Test
    void testGetCachedTenantRules() {
        UUID tenantId = UUID.randomUUID();
        List<RuleResponse> rules = Arrays.asList(
            new RuleResponse(UUID.randomUUID(), tenantId, "GET:/api/v1/**", AlgorithmType.TOKEN_BUCKET, 100, 60, true, LocalDateTime.now(), LocalDateTime.now())
        );

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("tenant_rules:" + tenantId)).thenReturn(rules);

        List<RuleResponse> result = redisConfigCache.getCachedTenantRules(tenantId);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(valueOperations).get("tenant_rules:" + tenantId);
    }

    @Test
    void testEvictTenant() {
        UUID tenantId = UUID.randomUUID();

        redisConfigCache.evictTenant(tenantId);

        verify(redisTemplate).delete("tenant:" + tenantId);
    }

    @Test
    void testEvictTenantByApiKey() {
        String apiKey = "sk_test_abc123";

        redisConfigCache.evictTenantByApiKey(apiKey);

        verify(redisTemplate).delete("tenant:api:" + apiKey);
    }

    @Test
    void testEvictRule() {
        UUID ruleId = UUID.randomUUID();

        redisConfigCache.evictRule(ruleId);

        verify(redisTemplate).delete("rule:" + ruleId);
    }

    @Test
    void testEvictTenantRules() {
        UUID tenantId = UUID.randomUUID();

        redisConfigCache.evictTenantRules(tenantId);

        verify(redisTemplate).delete("tenant_rules:" + tenantId);
    }

    @Test
    void testEvictAllTenantData() {
        UUID tenantId = UUID.randomUUID();

        redisConfigCache.evictAllTenantData(tenantId);

        verify(redisTemplate).delete("tenant:" + tenantId);
        verify(redisTemplate).delete("tenant_rules:" + tenantId);
    }

    @Test
    void testClearAllCache() {
        redisConfigCache.clearAllCache();

        verify(redisTemplate).keys("tenant:*");
        verify(redisTemplate).keys("rule:*");
        verify(redisTemplate).keys("tenant_rules:*");
    }
}