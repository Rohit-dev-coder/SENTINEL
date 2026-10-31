package com.sentinel.admin.repository;

import com.sentinel.admin.model.entity.Tenant;
import com.sentinel.common.enums.Tier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@EnabledIfSystemProperty(named = "docker.tests.enabled", matches = "true")
@ActiveProfiles("test")
class TenantRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    void testSaveAndFindById() {
        Tenant tenant = new Tenant("Test Tenant", "test-api-key", Tier.PREMIUM);
        tenant = entityManager.persistAndFlush(tenant);

        Optional<Tenant> found = tenantRepository.findById(tenant.getId());

        assertTrue(found.isPresent());
        assertEquals("Test Tenant", found.get().getName());
        assertEquals("test-api-key", found.get().getApiKey());
        assertEquals(Tier.PREMIUM, found.get().getTier());
    }

    @Test
    void testFindByApiKey() {
        Tenant tenant = new Tenant("Test Tenant", "unique-api-key", Tier.BASIC);
        entityManager.persistAndFlush(tenant);

        Optional<Tenant> found = tenantRepository.findByApiKey("unique-api-key");

        assertTrue(found.isPresent());
        assertEquals(tenant.getId(), found.get().getId());
    }

    @Test
    void testFindByTier() {
        Tenant tenant1 = new Tenant("Tenant 1", "key1", Tier.FREE);
        Tenant tenant2 = new Tenant("Tenant 2", "key2", Tier.FREE);
        Tenant tenant3 = new Tenant("Tenant 3", "key3", Tier.PREMIUM);
        
        entityManager.persist(tenant1);
        entityManager.persist(tenant2);
        entityManager.persist(tenant3);
        entityManager.flush();

        List<Tenant> freeTenants = tenantRepository.findByTier(Tier.FREE);
        List<Tenant> premiumTenants = tenantRepository.findByTier(Tier.PREMIUM);

        assertEquals(2, freeTenants.size());
        assertEquals(1, premiumTenants.size());
    }

    @Test
    void testFindByIsActiveTrue() {
        Tenant activeTenant = new Tenant("Active", "active-key", Tier.BASIC);
        activeTenant.setIsActive(true);
        
        Tenant inactiveTenant = new Tenant("Inactive", "inactive-key", Tier.BASIC);
        inactiveTenant.setIsActive(false);
        
        entityManager.persist(activeTenant);
        entityManager.persist(inactiveTenant);
        entityManager.flush();

        List<Tenant> activeTenants = tenantRepository.findByIsActiveTrue();

        assertEquals(1, activeTenants.size());
        assertEquals("Active", activeTenants.get(0).getName());
    }

    @Test
    void testFindActiveByTier() {
        Tenant activeFree = new Tenant("Active Free", "key1", Tier.FREE);
        activeFree.setIsActive(true);
        
        Tenant inactiveFree = new Tenant("Inactive Free", "key2", Tier.FREE);
        inactiveFree.setIsActive(false);
        
        Tenant activePremium = new Tenant("Active Premium", "key3", Tier.PREMIUM);
        activePremium.setIsActive(true);
        
        entityManager.persist(activeFree);
        entityManager.persist(inactiveFree);
        entityManager.persist(activePremium);
        entityManager.flush();

        List<Tenant> activeFreeTenants = tenantRepository.findActiveByTier(Tier.FREE);
        List<Tenant> activePremiumTenants = tenantRepository.findActiveByTier(Tier.PREMIUM);

        assertEquals(1, activeFreeTenants.size());
        assertEquals(1, activePremiumTenants.size());
    }

    @Test
    void testCountActiveTenants() {
        Tenant tenant1 = new Tenant("Tenant 1", "key1", Tier.BASIC);
        tenant1.setIsActive(true);
        
        Tenant tenant2 = new Tenant("Tenant 2", "key2", Tier.BASIC);
        tenant2.setIsActive(true);
        
        Tenant tenant3 = new Tenant("Tenant 3", "key3", Tier.BASIC);
        tenant3.setIsActive(false);
        
        entityManager.persist(tenant1);
        entityManager.persist(tenant2);
        entityManager.persist(tenant3);
        entityManager.flush();

        long count = tenantRepository.countActiveTenants();

        assertEquals(2, count);
    }

    @Test
    void testExistsByApiKey() {
        Tenant tenant = new Tenant("Test", "exists-key", Tier.FREE);
        entityManager.persistAndFlush(tenant);

        assertTrue(tenantRepository.existsByApiKey("exists-key"));
        assertFalse(tenantRepository.existsByApiKey("non-existent-key"));
    }

    @Test
    void testDeleteTenant() {
        Tenant tenant = new Tenant("To Delete", "delete-key", Tier.BASIC);
        tenant = entityManager.persistAndFlush(tenant);

        tenantRepository.deleteById(tenant.getId());

        Optional<Tenant> found = tenantRepository.findById(tenant.getId());
        assertFalse(found.isPresent());
    }

    @Test
    void testUpdateTenant() {
        Tenant tenant = new Tenant("Original", "update-key", Tier.FREE);
        tenant = entityManager.persistAndFlush(tenant);

        tenant.setName("Updated");
        tenant.setTier(Tier.PREMIUM);
        entityManager.persistAndFlush(tenant);

        Optional<Tenant> found = tenantRepository.findById(tenant.getId());
        assertTrue(found.isPresent());
        assertEquals("Updated", found.get().getName());
        assertEquals(Tier.PREMIUM, found.get().getTier());
    }
}