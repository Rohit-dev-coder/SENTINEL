package com.sentinel.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.admin.model.dto.TenantRequest;
import com.sentinel.admin.model.dto.TenantResponse;
import com.sentinel.admin.service.TenantService;
import com.sentinel.common.enums.Tier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TenantController.class)
class TenantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TenantService tenantService;

    @Test
    void testCreateTenant() throws Exception {
        TenantRequest request = new TenantRequest("Test Tenant", Tier.PREMIUM, "test@example.com");
        TenantResponse response = new TenantResponse(UUID.randomUUID(), "Test Tenant", "sk_test_abc123", Tier.PREMIUM, true, "test@example.com", null, null);

        when(tenantService.createTenant(any(TenantRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Tenant"))
                .andExpect(jsonPath("$.tier").value("PREMIUM"))
                .andExpect(jsonPath("$.isActive").value(true));

        verify(tenantService).createTenant(any(TenantRequest.class));
    }

    @Test
    void testGetTenantById() throws Exception {
        UUID tenantId = UUID.randomUUID();
        TenantResponse response = new TenantResponse(tenantId, "Test Tenant", "sk_test_abc123", Tier.BASIC, true, "test@example.com", null, null);

        when(tenantService.getTenantById(tenantId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/admin/tenants/" + tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(tenantId.toString()))
                .andExpect(jsonPath("$.name").value("Test Tenant"));

        verify(tenantService).getTenantById(tenantId);
    }

    @Test
    void testGetAllTenants() throws Exception {
        List<TenantResponse> tenants = Arrays.asList(
            new TenantResponse(UUID.randomUUID(), "Tenant 1", "key1", Tier.FREE, true, null, null, null),
            new TenantResponse(UUID.randomUUID(), "Tenant 2", "key2", Tier.PREMIUM, true, null, null, null)
        );

        when(tenantService.getAllTenants()).thenReturn(tenants);

        mockMvc.perform(get("/api/v1/admin/tenants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));

        verify(tenantService).getAllTenants();
    }

    @Test
    void testUpdateTenant() throws Exception {
        UUID tenantId = UUID.randomUUID();
        TenantRequest request = new TenantRequest("Updated Name", Tier.ENTERPRISE, "updated@example.com");
        TenantResponse response = new TenantResponse(tenantId, "Updated Name", "sk_test_abc123", Tier.ENTERPRISE, true, "updated@example.com", null, null);

        when(tenantService.updateTenant(eq(tenantId), any(TenantRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/admin/tenants/" + tenantId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.tier").value("ENTERPRISE"));

        verify(tenantService).updateTenant(eq(tenantId), any(TenantRequest.class));
    }

    @Test
    void testDeleteTenant() throws Exception {
        UUID tenantId = UUID.randomUUID();

        doNothing().when(tenantService).deleteTenant(tenantId);

        mockMvc.perform(delete("/api/v1/admin/tenants/" + tenantId))
                .andExpect(status().isNoContent());

        verify(tenantService).deleteTenant(tenantId);
    }

    @Test
    void testActivateTenant() throws Exception {
        UUID tenantId = UUID.randomUUID();

        doNothing().when(tenantService).activateTenant(tenantId);

        mockMvc.perform(post("/api/v1/admin/tenants/" + tenantId + "/activate"))
                .andExpect(status().isOk());

        verify(tenantService).activateTenant(tenantId);
    }

    @Test
    void testDeactivateTenant() throws Exception {
        UUID tenantId = UUID.randomUUID();

        doNothing().when(tenantService).deactivateTenant(tenantId);

        mockMvc.perform(post("/api/v1/admin/tenants/" + tenantId + "/deactivate"))
                .andExpect(status().isOk());

        verify(tenantService).deactivateTenant(tenantId);
    }
}