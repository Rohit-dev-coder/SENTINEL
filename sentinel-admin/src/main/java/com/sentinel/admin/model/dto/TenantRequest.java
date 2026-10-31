package com.sentinel.admin.model.dto;

import com.sentinel.common.enums.Tier;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class TenantRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Tier is required")
    private Tier tier;

    @Email(message = "Invalid email format")
    private String contactEmail;

    public TenantRequest() {
    }

    public TenantRequest(String name, Tier tier, String contactEmail) {
        this.name = name;
        this.tier = tier;
        this.contactEmail = contactEmail;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Tier getTier() {
        return tier;
    }

    public void setTier(Tier tier) {
        this.tier = tier;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }
}