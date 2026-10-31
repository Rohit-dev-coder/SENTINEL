package com.sentinel.admin.repository;

import com.sentinel.admin.model.entity.RateLimitEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface RateLimitEventRepository extends JpaRepository<RateLimitEvent, UUID> {

    List<RateLimitEvent> findByTenantId(UUID tenantId);

    List<RateLimitEvent> findByTenantIdAndCreatedAtBetween(UUID tenantId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT e FROM RateLimitEvent e WHERE e.tenantId = :tenantId AND e.createdAt >= :start AND e.createdAt <= :end")
    List<RateLimitEvent> findByTenantIdInTimeRange(@Param("tenantId") UUID tenantId, 
                                                   @Param("start") LocalDateTime start, 
                                                   @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(e) FROM RateLimitEvent e WHERE e.tenantId = :tenantId AND e.createdAt >= :start AND e.createdAt <= :end")
    long countByTenantIdInTimeRange(@Param("tenantId") UUID tenantId, 
                                    @Param("start") LocalDateTime start, 
                                    @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(e) FROM RateLimitEvent e WHERE e.tenantId = :tenantId AND e.allowed = false AND e.createdAt >= :start AND e.createdAt <= :end")
    long countDeniedByTenantIdInTimeRange(@Param("tenantId") UUID tenantId, 
                                        @Param("start") LocalDateTime start, 
                                        @Param("end") LocalDateTime end);

    @Query("SELECT e FROM RateLimitEvent e WHERE e.createdAt >= :start AND e.createdAt <= :end ORDER BY e.createdAt DESC")
    List<RateLimitEvent> findAllInTimeRange(@Param("start") LocalDateTime start, 
                                          @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(e) FROM RateLimitEvent e WHERE e.allowed = false")
    long countDeniedEvents();
}