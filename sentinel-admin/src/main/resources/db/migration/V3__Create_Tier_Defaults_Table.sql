-- Create tier defaults table for fallback rules
CREATE TABLE tier_defaults (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tier            VARCHAR(20) NOT NULL,
                    -- FREE, BASIC, PREMIUM, ENTERPRISE
                    CHECK (tier IN ('FREE', 'BASIC', 'PREMIUM', 'ENTERPRISE')),
    algorithm       VARCHAR(30) NOT NULL DEFAULT 'TOKEN_BUCKET',
                    CHECK (algorithm IN ('TOKEN_BUCKET', 'SLIDING_WINDOW_LOG', 
                                        'FIXED_WINDOW_COUNTER', 'SLIDING_WINDOW_COUNTER')),
    max_requests    INT NOT NULL CHECK (max_requests > 0),
    window_seconds  INT NOT NULL DEFAULT 60 CHECK (window_seconds > 0),
    
    UNIQUE(tier)
);

-- Insert default tier limits
INSERT INTO tier_defaults (tier, algorithm, max_requests, window_seconds) VALUES
    ('FREE', 'TOKEN_BUCKET', 10, 60),
    ('BASIC', 'TOKEN_BUCKET', 100, 60),
    ('PREMIUM', 'SLIDING_WINDOW_COUNTER', 1000, 60),
    ('ENTERPRISE', 'SLIDING_WINDOW_COUNTER', 10000, 60);