package com.sentinel.admin.repository;

import com.sentinel.admin.model.entity.Tenant;
import com.sentinel.common.enums.Tier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    Optional<Tenant> findByApiKey(String apiKey);

    List<Tenant> findByTier(Tier tier);

    List<Tenant> findByIsActiveTrue();

    @Query("SELECT t FROM Tenant t WHERE t.isActive = true AND t.tier = :tier")
    List<Tenant> findActiveByTier(@Param("tier") Tier tier);

    @Query("SELECT COUNT(t) FROM Tenant t WHERE t.isActive = true")
    long countActiveTenants();

    boolean existsByApiKey(String apiKey);
}