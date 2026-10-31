package com.sentinel.admin.model.dto;

import com.sentinel.common.enums.Tier;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TenantRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testValidTenantRequest() {
        TenantRequest request = new TenantRequest("Test Tenant", Tier.PREMIUM, "test@example.com");
        
        assertEquals("Test Tenant", request.getName());
        assertEquals(Tier.PREMIUM, request.getTier());
        assertEquals("test@example.com", request.getContactEmail());
    }

    @Test
    void testTenantRequestSettersAndGetters() {
        TenantRequest request = new TenantRequest();
        
        request.setName("Updated Name");
        request.setTier(Tier.ENTERPRISE);
        request.setContactEmail("updated@example.com");
        
        assertEquals("Updated Name", request.getName());
        assertEquals(Tier.ENTERPRISE, request.getTier());
        assertEquals("updated@example.com", request.getContactEmail());
    }

    @Test
    void testTenantRequestWithNullEmail() {
        TenantRequest request = new TenantRequest("Test Tenant", Tier.BASIC, null);
        
        assertEquals("Test Tenant", request.getName());
        assertEquals(Tier.BASIC, request.getTier());
        assertNull(request.getContactEmail());
    }

    @Test
    void testTenantRequestValidation() {
        TenantRequest request = new TenantRequest();
        
        // Valid data
        request.setName("Valid Name");
        request.setTier(Tier.FREE);
        request.setContactEmail("valid@example.com");
        
        var violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Valid request should have no violations");
    }

    @Test
    void testTenantRequestInvalidEmail() {
        TenantRequest request = new TenantRequest();
        request.setName("Test");
        request.setTier(Tier.FREE);
        request.setContactEmail("invalid-email");
        
        var violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Invalid email should have violations");
    }

    @Test
    void testTenantRequestBlankName() {
        TenantRequest request = new TenantRequest();
        request.setName("");
        request.setTier(Tier.FREE);
        
        var violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Blank name should have violations");
    }

    @Test
    void testTenantRequestNullTier() {
        TenantRequest request = new TenantRequest();
        request.setName("Test");
        request.setTier(null);
        
        var violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Null tier should have violations");
    }
}