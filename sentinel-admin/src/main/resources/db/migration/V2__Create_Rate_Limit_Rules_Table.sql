-- Create rate limit rules table
CREATE TABLE rate_limit_rules (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    endpoint_pattern VARCHAR(255) NOT NULL,
                    -- e.g., "GET:/api/v1/users/**"
    algorithm       VARCHAR(30) NOT NULL DEFAULT 'TOKEN_BUCKET',
                    -- TOKEN_BUCKET, SLIDING_WINDOW_LOG,
                    -- FIXED_WINDOW_COUNTER, SLIDING_WINDOW_COUNTER
                    CHECK (algorithm IN ('TOKEN_BUCKET', 'SLIDING_WINDOW_LOG', 
                                        'FIXED_WINDOW_COUNTER', 'SLIDING_WINDOW_COUNTER')),
    max_requests    INT NOT NULL DEFAULT 100 CHECK (max_requests > 0),
    window_seconds  INT NOT NULL DEFAULT 60 CHECK (window_seconds > 0),
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW(),
    
    UNIQUE(tenant_id, endpoint_pattern)
);

-- Create index on tenant_id for fast tenant-specific rule lookups
CREATE INDEX idx_rules_tenant ON rate_limit_rules(tenant_id);
-- Create index on endpoint_pattern for pattern matching
CREATE INDEX idx_rules_endpoint ON rate_limit_rules(endpoint_pattern);
-- Create index on is_active for active rule queries
CREATE INDEX idx_rules_active ON rate_limit_rules(is_active) WHERE is_active = TRUE;
-- Create composite index for tenant + active queries
CREATE INDEX idx_rules_tenant_active ON rate_limit_rules(tenant_id, is_active) WHERE is_active = TRUE;