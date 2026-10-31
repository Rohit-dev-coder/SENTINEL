package com.sentinel.admin.model.dto;

import com.sentinel.common.enums.AlgorithmType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class RuleRequest {

    @NotNull(message = "Tenant ID is required")
    private UUID tenantId;

    @NotBlank(message = "Endpoint pattern is required")
    private String endpointPattern;

    @NotNull(message = "Algorithm is required")
    private AlgorithmType algorithm;

    @NotNull(message = "Max requests is required")
    @Min(value = 1, message = "Max requests must be at least 1")
    private Integer maxRequests;

    @NotNull(message = "Window seconds is required")
    @Min(value = 1, message = "Window seconds must be at least 1")
    private Integer windowSeconds;

    public RuleRequest() {
    }

    public RuleRequest(UUID tenantId, String endpointPattern, AlgorithmType algorithm, 
                       Integer maxRequests, Integer windowSeconds) {
        this.tenantId = tenantId;
        this.endpointPattern = endpointPattern;
        this.algorithm = algorithm;
        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;
    }

    // Getters and Setters
    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public String getEndpointPattern() {
        return endpointPattern;
    }

    public void setEndpointPattern(String endpointPattern) {
        this.endpointPattern = endpointPattern;
    }

    public AlgorithmType getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(AlgorithmType algorithm) {
        this.algorithm = algorithm;
    }

    public Integer getMaxRequests() {
        return maxRequests;
    }

    public void setMaxRequests(Integer maxRequests) {
        this.maxRequests = maxRequests;
    }

    public Integer getWindowSeconds() {
        return windowSeconds;
    }

    public void setWindowSeconds(Integer windowSeconds) {
        this.windowSeconds = windowSeconds;
    }
}