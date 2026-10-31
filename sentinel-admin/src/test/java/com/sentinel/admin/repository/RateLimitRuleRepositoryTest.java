package com.sentinel.admin.repository;

import com.sentinel.admin.model.entity.RateLimitRule;
import com.sentinel.common.enums.AlgorithmType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@EnabledIfSystemProperty(named = "docker.tests.enabled", matches = "true")
class RateLimitRuleRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RateLimitRuleRepository ruleRepository;

    @Test
    void testSaveAndFindById() {
        UUID tenantId = UUID.randomUUID();
        RateLimitRule rule = new RateLimitRule(tenantId, "GET:/api/v1/users/**", 
                                            AlgorithmType.TOKEN_BUCKET, 100, 60);
        rule = entityManager.persistAndFlush(rule);

        Optional<RateLimitRule> found = ruleRepository.findById(rule.getId());

        assertTrue(found.isPresent());
        assertEquals(tenantId, found.get().getTenantId());
        assertEquals("GET:/api/v1/users/**", found.get().getEndpointPattern());
        assertEquals(AlgorithmType.TOKEN_BUCKET, found.get().getAlgorithm());
    }

    @Test
    void testFindByTenantId() {
        UUID tenantId = UUID.randomUUID();
        
        RateLimitRule rule1 = new RateLimitRule(tenantId, "GET:/api/v1/users/**", AlgorithmType.TOKEN_BUCKET, 100, 60);
        RateLimitRule rule2 = new RateLimitRule(tenantId, "POST:/api/v1/orders/**", AlgorithmType.SLIDING_WINDOW_LOG, 50, 60);
        
        entityManager.persist(rule1);
        entityManager.persist(rule2);
        entityManager.flush();

        List<RateLimitRule> rules = ruleRepository.findByTenantId(tenantId);

        assertEquals(2, rules.size());
    }

    @Test
    void testFindByTenantIdAndEndpointPattern() {
        UUID tenantId = UUID.randomUUID();
        RateLimitRule rule = new RateLimitRule(tenantId, "GET:/api/v1/users/**", AlgorithmType.TOKEN_BUCKET, 100, 60);
        entityManager.persistAndFlush(rule);

        Optional<RateLimitRule> found = ruleRepository.findByTenantIdAndEndpointPattern(tenantId, "GET:/api/v1/users/**");

        assertTrue(found.isPresent());
        assertEquals(rule.getId(), found.get().getId());
    }

    @Test
    void testFindByTenantIdAndIsActiveTrue() {
        UUID tenantId = UUID.randomUUID();
        
        RateLimitRule activeRule = new RateLimitRule(tenantId, "GET:/api/v1/users/**", AlgorithmType.TOKEN_BUCKET, 100, 60);
        activeRule.setIsActive(true);
        
        RateLimitRule inactiveRule = new RateLimitRule(tenantId, "POST:/api/v1/orders/**", AlgorithmType.SLIDING_WINDOW_LOG, 50, 60);
        inactiveRule.setIsActive(false);
        
        entityManager.persist(activeRule);
        entityManager.persist(inactiveRule);
        entityManager.flush();

        List<RateLimitRule> activeRules = ruleRepository.findByTenantIdAndIsActiveTrue(tenantId);

        assertEquals(1, activeRules.size());
        assertTrue(activeRules.get(0).getIsActive());
    }

    @Test
    void testFindByAlgorithm() {
        UUID tenantId1 = UUID.randomUUID();
        UUID tenantId2 = UUID.randomUUID();
        
        RateLimitRule tokenBucket1 = new RateLimitRule(tenantId1, "GET:/api/v1/**", AlgorithmType.TOKEN_BUCKET, 100, 60);
        RateLimitRule tokenBucket2 = new RateLimitRule(tenantId2, "POST:/api/v1/**", AlgorithmType.TOKEN_BUCKET, 50, 60);
        RateLimitRule slidingWindow = new RateLimitRule(tenantId1, "PUT:/api/v1/**", AlgorithmType.SLIDING_WINDOW_COUNTER, 200, 60);
        
        entityManager.persist(tokenBucket1);
        entityManager.persist(tokenBucket2);
        entityManager.persist(slidingWindow);
        entityManager.flush();

        List<RateLimitRule> tokenBucketRules = ruleRepository.findByAlgorithm(AlgorithmType.TOKEN_BUCKET);
        List<RateLimitRule> slidingWindowRules = ruleRepository.findByAlgorithm(AlgorithmType.SLIDING_WINDOW_COUNTER);

        assertEquals(2, tokenBucketRules.size());
        assertEquals(1, slidingWindowRules.size());
    }

    @Test
    void testFindAllActive() {
        UUID tenantId = UUID.randomUUID();
        
        RateLimitRule activeRule1 = new RateLimitRule(tenantId, "GET:/api/v1/users/**", AlgorithmType.TOKEN_BUCKET, 100, 60);
        activeRule1.setIsActive(true);
        
        RateLimitRule activeRule2 = new RateLimitRule(tenantId, "POST:/api/v1/orders/**", AlgorithmType.SLIDING_WINDOW_LOG, 50, 60);
        activeRule2.setIsActive(true);
        
        RateLimitRule inactiveRule = new RateLimitRule(tenantId, "PUT:/api/v1/products/**", AlgorithmType.FIXED_WINDOW_COUNTER, 200, 60);
        inactiveRule.setIsActive(false);
        
        entityManager.persist(activeRule1);
        entityManager.persist(activeRule2);
        entityManager.persist(inactiveRule);
        entityManager.flush();

        List<RateLimitRule> activeRules = ruleRepository.findAllActive();

        assertEquals(2, activeRules.size());
    }

    @Test
    void testCountActiveRules() {
        UUID tenantId = UUID.randomUUID();
        
        RateLimitRule activeRule1 = new RateLimitRule(tenantId, "GET:/api/v1/**", AlgorithmType.TOKEN_BUCKET, 100, 60);
        activeRule1.setIsActive(true);
        
        RateLimitRule activeRule2 = new RateLimitRule(tenantId, "POST:/api/v1/**", AlgorithmType.SLIDING_WINDOW_LOG, 50, 60);
        activeRule2.setIsActive(true);
        
        RateLimitRule inactiveRule = new RateLimitRule(tenantId, "PUT:/api/v1/**", AlgorithmType.FIXED_WINDOW_COUNTER, 200, 60);
        inactiveRule.setIsActive(false);
        
        entityManager.persist(activeRule1);
        entityManager.persist(activeRule2);
        entityManager.persist(inactiveRule);
        entityManager.flush();

        long count = ruleRepository.countActiveRules();

        assertEquals(2, count);
    }

    @Test
    void testExistsByTenantIdAndEndpointPattern() {
        UUID tenantId = UUID.randomUUID();
        RateLimitRule rule = new RateLimitRule(tenantId, "GET:/api/v1/users/**", AlgorithmType.TOKEN_BUCKET, 100, 60);
        entityManager.persistAndFlush(rule);

        assertTrue(ruleRepository.existsByTenantIdAndEndpointPattern(tenantId, "GET:/api/v1/users/**"));
        assertFalse(ruleRepository.existsByTenantIdAndEndpointPattern(tenantId, "POST:/api/v1/orders/**"));
    }

    @Test
    void testDeleteRule() {
        UUID tenantId = UUID.randomUUID();
        RateLimitRule rule = new RateLimitRule(tenantId, "GET:/api/v1/users/**", AlgorithmType.TOKEN_BUCKET, 100, 60);
        rule = entityManager.persistAndFlush(rule);

        ruleRepository.deleteById(rule.getId());

        Optional<RateLimitRule> found = ruleRepository.findById(rule.getId());
        assertFalse(found.isPresent());
    }
}