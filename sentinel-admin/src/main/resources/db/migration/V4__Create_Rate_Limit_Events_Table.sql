-- Create rate limit events table for audit logging
CREATE TABLE rate_limit_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID REFERENCES tenants(id) ON DELETE SET NULL,
    endpoint        VARCHAR(255),
    algorithm       VARCHAR(30),
    allowed         BOOLEAN NOT NULL,
    remaining       INT,
    ip_address      VARCHAR(45),
    created_at      TIMESTAMP DEFAULT NOW()
);

-- Create index on tenant_id for tenant-specific analytics
CREATE INDEX idx_events_tenant ON rate_limit_events(tenant_id);
-- Create index on created_at for time-based queries
CREATE INDEX idx_events_time ON rate_limit_events(created_at);
-- Create composite index for tenant + time analytics
CREATE INDEX idx_events_tenant_time ON rate_limit_events(tenant_id, created_at);
-- Create index on allowed for denial rate analytics
CREATE INDEX idx_events_allowed ON rate_limit_events(allowed) WHERE allowed = FALSE;