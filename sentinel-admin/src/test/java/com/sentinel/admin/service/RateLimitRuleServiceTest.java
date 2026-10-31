package com.sentinel.admin.service;

import com.sentinel.admin.cache.RedisConfigCache;
import com.sentinel.admin.model.dto.RuleRequest;
import com.sentinel.admin.model.dto.RuleResponse;
import com.sentinel.admin.model.entity.RateLimitRule;
import com.sentinel.admin.repository.RateLimitRuleRepository;
import com.sentinel.common.enums.AlgorithmType;
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
class RateLimitRuleServiceTest {

    @Mock
    private RateLimitRuleRepository ruleRepository;

    @Mock
    private RedisConfigCache redisConfigCache;

    @InjectMocks
    private RateLimitRuleService ruleService;

    @Test
    void testCreateRule() {
        UUID tenantId = UUID.randomUUID();
        RuleRequest request = new RuleRequest(tenantId, "GET:/api/v1/users/**", 
                                            AlgorithmType.TOKEN_BUCKET, 100, 60);
        RateLimitRule savedRule = new RateLimitRule(tenantId, "GET:/api/v1/users/**", 
                                            AlgorithmType.TOKEN_BUCKET, 100, 60);
        savedRule.setId(UUID.randomUUID());

        when(ruleRepository.existsByTenantIdAndEndpointPattern(tenantId, "GET:/api/v1/users/**")).thenReturn(false);
        when(ruleRepository.save(any(RateLimitRule.class))).thenReturn(savedRule);

        RuleResponse response = ruleService.createRule(request);

        assertNotNull(response);
        assertEquals(tenantId, response.getTenantId());
        assertEquals("GET:/api/v1/users/**", response.getEndpointPattern());
        assertEquals(AlgorithmType.TOKEN_BUCKET, response.getAlgorithm());
        verify(ruleRepository).save(any(RateLimitRule.class));
        verify(redisConfigCache).cacheRule(any(RuleResponse.class));
        verify(redisConfigCache).evictTenantRules(tenantId);
    }

    @Test
    void testCreateRuleDuplicate() {
        UUID tenantId = UUID.randomUUID();
        RuleRequest request = new RuleRequest(tenantId, "GET:/api/v1/users/**", 
                                            AlgorithmType.TOKEN_BUCKET, 100, 60);

        when(ruleRepository.existsByTenantIdAndEndpointPattern(tenantId, "GET:/api/v1/users/**")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> ruleService.createRule(request));
        verify(ruleRepository, never()).save(any(RateLimitRule.class));
    }

    @Test
    void testGetRuleById() {
        UUID ruleId = UUID.randomUUID();
        RateLimitRule rule = new RateLimitRule(UUID.randomUUID(), "GET:/api/v1/**", 
                                            AlgorithmType.SLIDING_WINDOW_LOG, 50, 60);
        rule.setId(ruleId);

        when(redisConfigCache.getCachedRule(ruleId)).thenReturn(null);
        when(ruleRepository.findById(ruleId)).thenReturn(Optional.of(rule));

        RuleResponse response = ruleService.getRuleById(ruleId);

        assertNotNull(response);
        assertEquals(ruleId, response.getId());
        assertEquals(AlgorithmType.SLIDING_WINDOW_LOG, response.getAlgorithm());
        verify(redisConfigCache).getCachedRule(ruleId);
        verify(ruleRepository).findById(ruleId);
        verify(redisConfigCache).cacheRule(any(RuleResponse.class));
    }

    @Test
    void testGetRuleByIdFromCache() {
        UUID ruleId = UUID.randomUUID();
        RuleResponse cachedResponse = new RuleResponse(ruleId, UUID.randomUUID(), "GET:/api/v1/**", AlgorithmType.TOKEN_BUCKET, 100, 60, true, null, null);

        when(redisConfigCache.getCachedRule(ruleId)).thenReturn(cachedResponse);

        RuleResponse response = ruleService.getRuleById(ruleId);

        assertNotNull(response);
        assertEquals(ruleId, response.getId());
        verify(redisConfigCache).getCachedRule(ruleId);
        verify(ruleRepository, never()).findById(any());
    }

    @Test
    void testGetRulesByTenantId() {
        UUID tenantId = UUID.randomUUID();
        RateLimitRule rule1 = new RateLimitRule(tenantId, "GET:/api/v1/users/**", AlgorithmType.TOKEN_BUCKET, 100, 60);
        RateLimitRule rule2 = new RateLimitRule(tenantId, "POST:/api/v1/orders/**", AlgorithmType.SLIDING_WINDOW_LOG, 50, 60);

        when(redisConfigCache.getCachedTenantRules(tenantId)).thenReturn(null);
        when(ruleRepository.findByTenantId(tenantId)).thenReturn(Arrays.asList(rule1, rule2));

        List<RuleResponse> responses = ruleService.getRulesByTenantId(tenantId);

        assertEquals(2, responses.size());
        verify(redisConfigCache).getCachedTenantRules(tenantId);
        verify(ruleRepository).findByTenantId(tenantId);
        verify(redisConfigCache).cacheTenantRules(eq(tenantId), anyList());
    }

    @Test
    void testGetActiveRulesByTenantId() {
        UUID tenantId = UUID.randomUUID();
        RateLimitRule activeRule = new RateLimitRule(tenantId, "GET:/api/v1/**", AlgorithmType.TOKEN_BUCKET, 100, 60);
        activeRule.setIsActive(true);

        when(redisConfigCache.getCachedTenantRules(tenantId)).thenReturn(null);
        when(ruleRepository.findByTenantIdAndIsActiveTrue(tenantId)).thenReturn(Arrays.asList(activeRule));

        List<RuleResponse> responses = ruleService.getActiveRulesByTenantId(tenantId);

        assertEquals(1, responses.size());
        assertTrue(responses.get(0).getIsActive());
        verify(redisConfigCache).getCachedTenantRules(tenantId);
        verify(ruleRepository).findByTenantIdAndIsActiveTrue(tenantId);
        verify(redisConfigCache).cacheTenantRules(eq(tenantId), anyList());
    }

    @Test
    void testGetAllRules() {
        RateLimitRule rule1 = new RateLimitRule(UUID.randomUUID(), "GET:/api/v1/**", AlgorithmType.TOKEN_BUCKET, 100, 60);
        RateLimitRule rule2 = new RateLimitRule(UUID.randomUUID(), "POST:/api/v1/**", AlgorithmType.SLIDING_WINDOW_LOG, 50, 60);

        when(ruleRepository.findAll()).thenReturn(Arrays.asList(rule1, rule2));

        List<RuleResponse> responses = ruleService.getAllRules();

        assertEquals(2, responses.size());
        verify(ruleRepository).findAll();
    }

    @Test
    void testGetRulesByAlgorithm() {
        RateLimitRule tokenBucketRule = new RateLimitRule(UUID.randomUUID(), "GET:/api/v1/**", AlgorithmType.TOKEN_BUCKET, 100, 60);
        RateLimitRule slidingWindowRule = new RateLimitRule(UUID.randomUUID(), "POST:/api/v1/**", AlgorithmType.SLIDING_WINDOW_COUNTER, 200, 60);

        when(ruleRepository.findByAlgorithm(AlgorithmType.TOKEN_BUCKET)).thenReturn(Arrays.asList(tokenBucketRule));

        List<RuleResponse> responses = ruleService.getRulesByAlgorithm(AlgorithmType.TOKEN_BUCKET);

        assertEquals(1, responses.size());
        assertEquals(AlgorithmType.TOKEN_BUCKET, responses.get(0).getAlgorithm());
        verify(ruleRepository).findByAlgorithm(AlgorithmType.TOKEN_BUCKET);
    }

    @Test
    void testUpdateRule() {
        UUID ruleId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        RateLimitRule existingRule = new RateLimitRule(tenantId, "GET:/api/v1/**", AlgorithmType.TOKEN_BUCKET, 100, 60);
        existingRule.setId(ruleId);

        RateLimitRule updatedRule = new RateLimitRule(tenantId, "POST:/api/v1/**", AlgorithmType.SLIDING_WINDOW_LOG, 50, 60);
        updatedRule.setId(ruleId);

        RuleRequest request = new RuleRequest(tenantId, "POST:/api/v1/**", AlgorithmType.SLIDING_WINDOW_LOG, 50, 60);

        when(ruleRepository.findById(ruleId)).thenReturn(Optional.of(existingRule));
        when(ruleRepository.save(any(RateLimitRule.class))).thenReturn(updatedRule);

        RuleResponse response = ruleService.updateRule(ruleId, request);

        assertEquals("POST:/api/v1/**", response.getEndpointPattern());
        assertEquals(AlgorithmType.SLIDING_WINDOW_LOG, response.getAlgorithm());
        verify(ruleRepository).save(any(RateLimitRule.class));
        verify(redisConfigCache).evictRule(ruleId);
        verify(redisConfigCache).cacheRule(any(RuleResponse.class));
        verify(redisConfigCache).evictTenantRules(tenantId);
    }

    @Test
    void testDeleteRule() {
        UUID ruleId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        RateLimitRule rule = new RateLimitRule(tenantId, "GET:/api/v1/**", AlgorithmType.TOKEN_BUCKET, 100, 60);
        rule.setId(ruleId);

        when(ruleRepository.findById(ruleId)).thenReturn(Optional.of(rule));
        doNothing().when(ruleRepository).deleteById(ruleId);

        ruleService.deleteRule(ruleId);

        verify(ruleRepository).findById(ruleId);
        verify(ruleRepository).deleteById(ruleId);
        verify(redisConfigCache).evictRule(ruleId);
        verify(redisConfigCache).evictTenantRules(tenantId);
    }

    @Test
    void testActivateRule() {
        UUID ruleId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        RateLimitRule rule = new RateLimitRule(tenantId, "GET:/api/v1/**", AlgorithmType.TOKEN_BUCKET, 100, 60);
        rule.setId(ruleId);
        rule.setIsActive(false);

        when(ruleRepository.findById(ruleId)).thenReturn(Optional.of(rule));
        when(ruleRepository.save(any(RateLimitRule.class))).thenReturn(rule);

        ruleService.activateRule(ruleId);

        assertTrue(rule.getIsActive());
        verify(ruleRepository).save(rule);
        verify(redisConfigCache).evictRule(ruleId);
        verify(redisConfigCache).cacheRule(any(RuleResponse.class));
        verify(redisConfigCache).evictTenantRules(tenantId);
    }

    @Test
    void testDeactivateRule() {
        UUID ruleId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        RateLimitRule rule = new RateLimitRule(tenantId, "GET:/api/v1/**", AlgorithmType.TOKEN_BUCKET, 100, 60);
        rule.setId(ruleId);
        rule.setIsActive(true);

        when(ruleRepository.findById(ruleId)).thenReturn(Optional.of(rule));
        when(ruleRepository.save(any(RateLimitRule.class))).thenReturn(rule);

        ruleService.deactivateRule(ruleId);

        assertFalse(rule.getIsActive());
        verify(ruleRepository).save(rule);
        verify(redisConfigCache).evictRule(ruleId);
        verify(redisConfigCache).cacheRule(any(RuleResponse.class));
        verify(redisConfigCache).evictTenantRules(tenantId);
    }
}