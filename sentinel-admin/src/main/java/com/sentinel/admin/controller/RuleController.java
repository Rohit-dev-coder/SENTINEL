package com.sentinel.admin.controller;

import com.sentinel.admin.model.dto.RuleRequest;
import com.sentinel.admin.model.dto.RuleResponse;
import com.sentinel.admin.service.RateLimitRuleService;
import com.sentinel.common.enums.AlgorithmType;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/rules")
public class RuleController {

    @Autowired
    private RateLimitRuleService ruleService;

    @PostMapping
    public ResponseEntity<RuleResponse> createRule(@Valid @RequestBody RuleRequest request) {
        RuleResponse response = ruleService.createRule(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RuleResponse> getRuleById(@PathVariable UUID id) {
        RuleResponse response = ruleService.getRuleById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<RuleResponse>> getAllRules() {
        List<RuleResponse> rules = ruleService.getAllRules();
        return ResponseEntity.ok(rules);
    }

    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<List<RuleResponse>> getRulesByTenantId(@PathVariable UUID tenantId) {
        List<RuleResponse> rules = ruleService.getRulesByTenantId(tenantId);
        return ResponseEntity.ok(rules);
    }

    @GetMapping("/tenant/{tenantId}/active")
    public ResponseEntity<List<RuleResponse>> getActiveRulesByTenantId(@PathVariable UUID tenantId) {
        List<RuleResponse> rules = ruleService.getActiveRulesByTenantId(tenantId);
        return ResponseEntity.ok(rules);
    }

    @GetMapping("/algorithm/{algorithm}")
    public ResponseEntity<List<RuleResponse>> getRulesByAlgorithm(@PathVariable AlgorithmType algorithm) {
        List<RuleResponse> rules = ruleService.getRulesByAlgorithm(algorithm);
        return ResponseEntity.ok(rules);
    }

    @GetMapping("/active")
    public ResponseEntity<List<RuleResponse>> getAllActiveRules() {
        List<RuleResponse> rules = ruleService.getAllActiveRules();
        return ResponseEntity.ok(rules);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RuleResponse> updateRule(@PathVariable UUID id, @Valid @RequestBody RuleRequest request) {
        RuleResponse response = ruleService.updateRule(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable UUID id) {
        ruleService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<Void> activateRule(@PathVariable UUID id) {
        ruleService.activateRule(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateRule(@PathVariable UUID id) {
        ruleService.deactivateRule(id);
        return ResponseEntity.ok().build();
    }
}