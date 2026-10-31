package com.sentinel.admin.repository;

import com.sentinel.admin.model.entity.RateLimitRule;
import com.sentinel.common.enums.AlgorithmType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RateLimitRuleRepository extends JpaRepository<RateLimitRule, UUID> {

    List<RateLimitRule> findByTenantId(UUID tenantId);

    Optional<RateLimitRule> findByTenantIdAndEndpointPattern(UUID tenantId, String endpointPattern);

    List<RateLimitRule> findByTenantIdAndIsActiveTrue(UUID tenantId);

    @Query("SELECT r FROM RateLimitRule r WHERE r.tenantId = :tenantId AND r.isActive = true AND r.endpointPattern LIKE :pattern")
    List<RateLimitRule> findActiveByTenantAndEndpointPattern(@Param("tenantId") UUID tenantId, 
                                                              @Param("pattern") String pattern);

    List<RateLimitRule> findByAlgorithm(AlgorithmType algorithm);

    @Query("SELECT r FROM RateLimitRule r WHERE r.isActive = true")
    List<RateLimitRule> findAllActive();

    @Query("SELECT COUNT(r) FROM RateLimitRule r WHERE r.isActive = true")
    long countActiveRules();

    boolean existsByTenantIdAndEndpointPattern(UUID tenantId, String endpointPattern);
}