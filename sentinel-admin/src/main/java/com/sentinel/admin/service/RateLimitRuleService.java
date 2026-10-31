package com.sentinel.admin.service;

import com.sentinel.admin.cache.RedisConfigCache;
import com.sentinel.admin.model.dto.RuleRequest;
import com.sentinel.admin.model.dto.RuleResponse;
import com.sentinel.admin.model.entity.RateLimitRule;
import com.sentinel.admin.repository.RateLimitRuleRepository;
import com.sentinel.common.enums.AlgorithmType;
import com.sentinel.admin.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class RateLimitRuleService {

    @Autowired
    private RateLimitRuleRepository ruleRepository;

    @Autowired(required = false)
    private RedisConfigCache redisConfigCache;

    public RuleResponse createRule(RuleRequest request) {
        // Check if rule already exists for this tenant and endpoint
        if (ruleRepository.existsByTenantIdAndEndpointPattern(request.getTenantId(), request.getEndpointPattern())) {
            throw new IllegalArgumentException("Rule already exists for this tenant and endpoint");
        }

        RateLimitRule rule = new RateLimitRule(
            request.getTenantId(),
            request.getEndpointPattern(),
            request.getAlgorithm(),
            request.getMaxRequests(),
            request.getWindowSeconds()
        );

        RateLimitRule savedRule = ruleRepository.save(rule);
        RuleResponse response = mapToResponse(savedRule);
        
        // Cache the new rule if Redis is available
        if (redisConfigCache != null) {
            redisConfigCache.cacheRule(response);
            redisConfigCache.evictTenantRules(request.getTenantId());
        }
        
        return response;
    }

    public RuleResponse getRuleById(UUID id) {
        // Try cache first if Redis is available
        if (redisConfigCache != null) {
            RuleResponse cached = redisConfigCache.getCachedRule(id);
            if (cached != null) {
                return cached;
            }
        }

        RateLimitRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rule not found: " + id));
        RuleResponse response = mapToResponse(rule);
        
        // Cache the rule if Redis is available
        if (redisConfigCache != null) {
            redisConfigCache.cacheRule(response);
        }
        
        return response;
    }

    public List<RuleResponse> getRulesByTenantId(UUID tenantId) {
        // Try cache first if Redis is available
        if (redisConfigCache != null) {
            List<RuleResponse> cached = redisConfigCache.getCachedTenantRules(tenantId);
            if (cached != null) {
                return cached;
            }
        }

        List<RuleResponse> rules = ruleRepository.findByTenantId(tenantId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        
        // Cache the rules if Redis is available
        if (redisConfigCache != null) {
            redisConfigCache.cacheTenantRules(tenantId, rules);
        }
        
        return rules;
    }

    public List<RuleResponse> getActiveRulesByTenantId(UUID tenantId) {
        // Try cache first if Redis is available
        if (redisConfigCache != null) {
            List<RuleResponse> cached = redisConfigCache.getCachedTenantRules(tenantId);
            if (cached != null) {
                return cached.stream()
                        .filter(RuleResponse::getIsActive)
                        .collect(Collectors.toList());
            }
        }

        List<RuleResponse> rules = ruleRepository.findByTenantIdAndIsActiveTrue(tenantId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        
        // Cache the rules if Redis is available
        if (redisConfigCache != null) {
            redisConfigCache.cacheTenantRules(tenantId, rules);
        }
        
        return rules;
    }

    public List<RuleResponse> getAllRules() {
        return ruleRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<RuleResponse> getRulesByAlgorithm(AlgorithmType algorithm) {
        return ruleRepository.findByAlgorithm(algorithm).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<RuleResponse> getAllActiveRules() {
        return ruleRepository.findAllActive().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public RuleResponse updateRule(UUID id, RuleRequest request) {
        RateLimitRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rule not found: " + id));

        rule.setEndpointPattern(request.getEndpointPattern());
        rule.setAlgorithm(request.getAlgorithm());
        rule.setMaxRequests(request.getMaxRequests());
        rule.setWindowSeconds(request.getWindowSeconds());

        RateLimitRule updatedRule = ruleRepository.save(rule);
        RuleResponse response = mapToResponse(updatedRule);
        
        // Update cache if Redis is available
        if (redisConfigCache != null) {
            redisConfigCache.evictRule(id);
            redisConfigCache.cacheRule(response);
            redisConfigCache.evictTenantRules(request.getTenantId());
        }
        
        return response;
    }

    public void deleteRule(UUID id) {
        RateLimitRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rule not found: " + id));
        
        // Evict from cache if Redis is available
        if (redisConfigCache != null) {
            redisConfigCache.evictRule(id);
            redisConfigCache.evictTenantRules(rule.getTenantId());
        }
        
        ruleRepository.deleteById(id);
    }

    public void activateRule(UUID id) {
        RateLimitRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rule not found: " + id));
        rule.setIsActive(true);
        RateLimitRule updatedRule = ruleRepository.save(rule);
        
        // Update cache if Redis is available
        if (redisConfigCache != null) {
            redisConfigCache.evictRule(id);
            redisConfigCache.cacheRule(mapToResponse(updatedRule));
            redisConfigCache.evictTenantRules(rule.getTenantId());
        }
    }

    public void deactivateRule(UUID id) {
        RateLimitRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rule not found: " + id));
        rule.setIsActive(false);
        RateLimitRule updatedRule = ruleRepository.save(rule);
        
        // Update cache if Redis is available
        if (redisConfigCache != null) {
            redisConfigCache.evictRule(id);
            redisConfigCache.cacheRule(mapToResponse(updatedRule));
            redisConfigCache.evictTenantRules(rule.getTenantId());
        }
    }

    private RuleResponse mapToResponse(RateLimitRule rule) {
        return new RuleResponse(
            rule.getId(),
            rule.getTenantId(),
            rule.getEndpointPattern(),
            rule.getAlgorithm(),
            rule.getMaxRequests(),
            rule.getWindowSeconds(),
            rule.getIsActive(),
            rule.getCreatedAt(),
            rule.getUpdatedAt()
        );
    }
}