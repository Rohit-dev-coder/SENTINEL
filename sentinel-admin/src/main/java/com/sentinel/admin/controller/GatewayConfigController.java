package com.sentinel.admin.controller;

import com.sentinel.admin.exception.ResourceNotFoundException;
import com.sentinel.admin.model.dto.GatewayConfigResponse;
import com.sentinel.admin.model.dto.RuleResponse;
import com.sentinel.admin.model.dto.TenantResponse;
import com.sentinel.admin.service.RateLimitRuleService;
import com.sentinel.admin.service.TenantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Internal endpoint used by the gateway to resolve a tenant's tier and rules from its API key.
 * The API key travels in a header so it is not written to access logs.
 */
@RestController
@RequestMapping("/api/v1/admin/gateway-config")
public class GatewayConfigController {

    @Autowired
    private TenantService tenantService;

    @Autowired
    private RateLimitRuleService ruleService;

    @GetMapping
    public ResponseEntity<GatewayConfigResponse> getConfig(@RequestHeader("X-Tenant-Api-Key") String apiKey) {
        TenantResponse tenant;
        try {
            tenant = tenantService.getTenantByApiKey(apiKey);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
        List<RuleResponse> rules = Boolean.TRUE.equals(tenant.getIsActive())
                ? ruleService.getActiveRulesByTenantId(tenant.getId())
                : List.of();
        return ResponseEntity.ok(new GatewayConfigResponse(
                Boolean.TRUE.equals(tenant.getIsActive()), tenant.getTier(), rules));
    }
}
