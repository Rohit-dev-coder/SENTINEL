package com.sentinel.admin.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.admin.model.dto.RuleResponse;
import com.sentinel.admin.model.dto.TenantResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "spring.data.redis.repositories.enabled", havingValue = "true", matchIfMissing = true)
public class RedisConfigCache {

    private static final String TENANT_KEY_PREFIX = "tenant:";
    private static final String RULE_KEY_PREFIX = "rule:";
    private static final String TENANT_RULES_KEY_PREFIX = "tenant_rules:";
    private static final long CACHE_TTL_HOURS = 24;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired(required = false)
    private ObjectMapper objectMapper;

    public void cacheTenant(TenantResponse tenant) {
        if (redisTemplate == null) return;
        String key = TENANT_KEY_PREFIX + tenant.getId();
        redisTemplate.opsForValue().set(key, tenant, CACHE_TTL_HOURS, TimeUnit.HOURS);
    }

    public TenantResponse getCachedTenant(UUID tenantId) {
        if (redisTemplate == null) return null;
        String key = TENANT_KEY_PREFIX + tenantId;
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached instanceof TenantResponse) {
            return (TenantResponse) cached;
        }
        return null;
    }

    public void cacheTenantByApiKey(String apiKey, TenantResponse tenant) {
        if (redisTemplate == null) return;
        String key = TENANT_KEY_PREFIX + "api:" + apiKey;
        redisTemplate.opsForValue().set(key, tenant, CACHE_TTL_HOURS, TimeUnit.HOURS);
    }

    public TenantResponse getCachedTenantByApiKey(String apiKey) {
        if (redisTemplate == null) return null;
        String key = TENANT_KEY_PREFIX + "api:" + apiKey;
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached instanceof TenantResponse) {
            return (TenantResponse) cached;
        }
        return null;
    }

    public void cacheRule(RuleResponse rule) {
        if (redisTemplate == null) return;
        String key = RULE_KEY_PREFIX + rule.getId();
        redisTemplate.opsForValue().set(key, rule, CACHE_TTL_HOURS, TimeUnit.HOURS);
    }

    public RuleResponse getCachedRule(UUID ruleId) {
        if (redisTemplate == null) return null;
        String key = RULE_KEY_PREFIX + ruleId;
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached instanceof RuleResponse) {
            return (RuleResponse) cached;
        }
        return null;
    }

    public void cacheTenantRules(UUID tenantId, List<RuleResponse> rules) {
        if (redisTemplate == null) return;
        String key = TENANT_RULES_KEY_PREFIX + tenantId;
        redisTemplate.opsForValue().set(key, rules, CACHE_TTL_HOURS, TimeUnit.HOURS);
    }

    public List<RuleResponse> getCachedTenantRules(UUID tenantId) {
        if (redisTemplate == null) return null;
        String key = TENANT_RULES_KEY_PREFIX + tenantId;
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached instanceof List) {
            return (List<RuleResponse>) cached;
        }
        return null;
    }

    public void evictTenant(UUID tenantId) {
        if (redisTemplate == null) return;
        String key = TENANT_KEY_PREFIX + tenantId;
        redisTemplate.delete(key);
    }

    public void evictTenantByApiKey(String apiKey) {
        if (redisTemplate == null) return;
        String key = TENANT_KEY_PREFIX + "api:" + apiKey;
        redisTemplate.delete(key);
    }

    public void evictRule(UUID ruleId) {
        if (redisTemplate == null) return;
        String key = RULE_KEY_PREFIX + ruleId;
        redisTemplate.delete(key);
    }

    public void evictTenantRules(UUID tenantId) {
        if (redisTemplate == null) return;
        String key = TENANT_RULES_KEY_PREFIX + tenantId;
        redisTemplate.delete(key);
    }

    public void evictAllTenantData(UUID tenantId) {
        evictTenant(tenantId);
        evictTenantRules(tenantId);
    }

    public void clearAllCache() {
        if (redisTemplate == null) return;
        for (String prefix : List.of(TENANT_KEY_PREFIX, RULE_KEY_PREFIX, TENANT_RULES_KEY_PREFIX)) {
            java.util.Set<String> keys = redisTemplate.keys(prefix + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        }
    }
}