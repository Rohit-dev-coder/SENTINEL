-- Create tenants table
CREATE TABLE tenants (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    api_key         VARCHAR(255) UNIQUE NOT NULL,
    tier            VARCHAR(20) NOT NULL DEFAULT 'FREE',
                    -- FREE, BASIC, PREMIUM, ENTERPRISE
                    CHECK (tier IN ('FREE', 'BASIC', 'PREMIUM', 'ENTERPRISE')),
    is_active       BOOLEAN DEFAULT TRUE,
    contact_email   VARCHAR(255),
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);

-- Create index on api_key for fast lookups
CREATE INDEX idx_tenants_api_key ON tenants(api_key);
-- Create index on tier for filtering
CREATE INDEX idx_tenants_tier ON tenants(tier);
-- Create index on is_active for active tenant queries
CREATE INDEX idx_tenants_active ON tenants(is_active) WHERE is_active = TRUE;