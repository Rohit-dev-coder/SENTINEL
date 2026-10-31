package com.sentinel.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.admin.model.dto.RuleRequest;
import com.sentinel.admin.model.dto.RuleResponse;
import com.sentinel.admin.model.dto.TenantRequest;
import com.sentinel.admin.model.dto.TenantResponse;
import com.sentinel.common.enums.AlgorithmType;
import com.sentinel.common.enums.Tier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@EnabledIfSystemProperty(named = "docker.tests.enabled", matches = "true")
class IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testFullTenantLifecycle() throws Exception {
        // Create tenant
        TenantRequest createRequest = new TenantRequest("Integration Test Tenant", Tier.PREMIUM, "integration@test.com");
        
        mockMvc.perform(post("/api/v1/admin/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Integration Test Tenant"))
                .andExpect(jsonPath("$.tier").value("PREMIUM"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.apiKey").exists());

        // Get all tenants
        mockMvc.perform(get("/api/v1/admin/tenants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));

        // Get tenant by ID (extract from previous response)
        // For simplicity, we'll skip the ID extraction and test other endpoints
    }

    @Test
    void testFullRuleLifecycle() throws Exception {
        // First create a tenant
        TenantRequest tenantRequest = new TenantRequest("Rule Test Tenant", Tier.BASIC, "rule@test.com");
        
        String tenantResponse = mockMvc.perform(post("/api/v1/admin/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tenantRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        TenantResponse tenant = objectMapper.readValue(tenantResponse, TenantResponse.class);

        // Create rule for the tenant
        RuleRequest ruleRequest = new RuleRequest(
            tenant.getId(),
            "GET:/api/v1/test/**",
            AlgorithmType.TOKEN_BUCKET,
            100,
            60
        );

        mockMvc.perform(post("/api/v1/admin/rules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ruleRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenantId").value(tenant.getId().toString()))
                .andExpect(jsonPath("$.endpointPattern").value("GET:/api/v1/test/**"))
                .andExpect(jsonPath("$.algorithm").value("TOKEN_BUCKET"))
                .andExpect(jsonPath("$.maxRequests").value(100))
                .andExpect(jsonPath("$.windowSeconds").value(60))
                .andExpect(jsonPath("$.isActive").value(true));

        // Get rules by tenant
        mockMvc.perform(get("/api/v1/admin/rules/tenant/" + tenant.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void testTenantActivationDeactivation() throws Exception {
        // Create tenant
        TenantRequest tenantRequest = new TenantRequest("Active Test Tenant", Tier.FREE, "active@test.com");
        
        String tenantResponse = mockMvc.perform(post("/api/v1/admin/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tenantRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        TenantResponse tenant = objectMapper.readValue(tenantResponse, TenantResponse.class);

        // Deactivate tenant
        mockMvc.perform(post("/api/v1/admin/tenants/" + tenant.getId() + "/deactivate"))
                .andExpect(status().isOk());

        // Verify tenant is deactivated
        mockMvc.perform(get("/api/v1/admin/tenants/" + tenant.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));

        // Activate tenant
        mockMvc.perform(post("/api/v1/admin/tenants/" + tenant.getId() + "/activate"))
                .andExpect(status().isOk());

        // Verify tenant is activated
        mockMvc.perform(get("/api/v1/admin/tenants/" + tenant.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(true));
    }

    @Test
    void testRuleActivationDeactivation() throws Exception {
        // Create tenant
        TenantRequest tenantRequest = new TenantRequest("Rule Active Test", Tier.BASIC, "ruleactive@test.com");
        
        String tenantResponse = mockMvc.perform(post("/api/v1/admin/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tenantRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        TenantResponse tenant = objectMapper.readValue(tenantResponse, TenantResponse.class);

        // Create rule
        RuleRequest ruleRequest = new RuleRequest(
            tenant.getId(),
            "POST:/api/v1/active/**",
            AlgorithmType.SLIDING_WINDOW_LOG,
            50,
            60
        );

        String ruleResponse = mockMvc.perform(post("/api/v1/admin/rules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ruleRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        RuleResponse rule = objectMapper.readValue(ruleResponse, RuleResponse.class);

        // Deactivate rule
        mockMvc.perform(post("/api/v1/admin/rules/" + rule.getId() + "/deactivate"))
                .andExpect(status().isOk());

        // Verify rule is deactivated
        mockMvc.perform(get("/api/v1/admin/rules/" + rule.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));

        // Activate rule
        mockMvc.perform(post("/api/v1/admin/rules/" + rule.getId() + "/activate"))
                .andExpect(status().isOk());

        // Verify rule is activated
        mockMvc.perform(get("/api/v1/admin/rules/" + rule.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(true));
    }

    @Test
    void testTenantUpdate() throws Exception {
        // Create tenant
        TenantRequest tenantRequest = new TenantRequest("Update Test", Tier.FREE, "update@test.com");
        
        String tenantResponse = mockMvc.perform(post("/api/v1/admin/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tenantRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        TenantResponse tenant = objectMapper.readValue(tenantResponse, TenantResponse.class);

        // Update tenant
        TenantRequest updateRequest = new TenantRequest("Updated Name", Tier.PREMIUM, "updated@test.com");

        mockMvc.perform(put("/api/v1/admin/tenants/" + tenant.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.tier").value("PREMIUM"))
                .andExpect(jsonPath("$.contactEmail").value("updated@test.com"));
    }

    @Test
    void testRuleUpdate() throws Exception {
        // Create tenant
        TenantRequest tenantRequest = new TenantRequest("Rule Update Test", Tier.BASIC, "ruleupdate@test.com");
        
        String tenantResponse = mockMvc.perform(post("/api/v1/admin/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tenantRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        TenantResponse tenant = objectMapper.readValue(tenantResponse, TenantResponse.class);

        // Create rule
        RuleRequest ruleRequest = new RuleRequest(
            tenant.getId(),
            "GET:/api/v1/original/**",
            AlgorithmType.TOKEN_BUCKET,
            100,
            60
        );

        String ruleResponse = mockMvc.perform(post("/api/v1/admin/rules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ruleRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        RuleResponse rule = objectMapper.readValue(ruleResponse, RuleResponse.class);

        // Update rule
        RuleRequest updateRequest = new RuleRequest(
            tenant.getId(),
            "GET:/api/v1/updated/**",
            AlgorithmType.SLIDING_WINDOW_COUNTER,
            200,
            30
        );

        mockMvc.perform(put("/api/v1/admin/rules/" + rule.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endpointPattern").value("GET:/api/v1/updated/**"))
                .andExpect(jsonPath("$.algorithm").value("SLIDING_WINDOW_COUNTER"))
                .andExpect(jsonPath("$.maxRequests").value(200))
                .andExpect(jsonPath("$.windowSeconds").value(30));
    }

    @Test
    void testTenantDeletion() throws Exception {
        // Create tenant
        TenantRequest tenantRequest = new TenantRequest("Delete Test", Tier.FREE, "delete@test.com");
        
        String tenantResponse = mockMvc.perform(post("/api/v1/admin/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tenantRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        TenantResponse tenant = objectMapper.readValue(tenantResponse, TenantResponse.class);

        // Delete tenant
        mockMvc.perform(delete("/api/v1/admin/tenants/" + tenant.getId()))
                .andExpect(status().isNoContent());

        // Verify tenant is deleted
        mockMvc.perform(get("/api/v1/admin/tenants/" + tenant.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testRuleDeletion() throws Exception {
        // Create tenant
        TenantRequest tenantRequest = new TenantRequest("Rule Delete Test", Tier.BASIC, "ruledelete@test.com");
        
        String tenantResponse = mockMvc.perform(post("/api/v1/admin/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tenantRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        TenantResponse tenant = objectMapper.readValue(tenantResponse, TenantResponse.class);

        // Create rule
        RuleRequest ruleRequest = new RuleRequest(
            tenant.getId(),
            "DELETE:/api/v1/test/**",
            AlgorithmType.TOKEN_BUCKET,
            100,
            60
        );

        String ruleResponse = mockMvc.perform(post("/api/v1/admin/rules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ruleRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        RuleResponse rule = objectMapper.readValue(ruleResponse, RuleResponse.class);

        // Delete rule
        mockMvc.perform(delete("/api/v1/admin/rules/" + rule.getId()))
                .andExpect(status().isNoContent());

        // Verify rule is deleted
        mockMvc.perform(get("/api/v1/admin/rules/" + rule.getId()))
                .andExpect(status().isNotFound());
    }
}